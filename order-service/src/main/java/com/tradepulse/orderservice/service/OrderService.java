package com.tradepulse.orderservice.service;

import com.tradepulse.orderservice.exception.IdempotencyException;
import com.tradepulse.orderservice.exception.ResourceNotFoundException;
import com.tradepulse.orderservice.model.AuditLog;
import com.tradepulse.orderservice.model.Order;
import com.tradepulse.orderservice.model.OrderStatus;
import com.tradepulse.orderservice.model.OrderType;
import com.tradepulse.orderservice.repository.AuditLogRepository;
import com.tradepulse.orderservice.repository.OrderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Service
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final PriceCacheService priceCacheService;
    private final OrderEventPublisher eventPublisher;
    private final OrderExecutionEngine executionEngine;
    private final AuditLogRepository auditLogRepository;
    private final StringRedisTemplate redisTemplate;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    public OrderService(OrderRepository orderRepository,
                        PriceCacheService priceCacheService,
                        OrderEventPublisher eventPublisher,
                        OrderExecutionEngine executionEngine,
                        AuditLogRepository auditLogRepository,
                        @Autowired(required = false) StringRedisTemplate redisTemplate) {
        this.orderRepository = orderRepository;
        this.priceCacheService = priceCacheService;
        this.eventPublisher = eventPublisher;
        this.executionEngine = executionEngine;
        this.auditLogRepository = auditLogRepository;
        this.redisTemplate = redisTemplate;
    }

    @Transactional
    public Order placeOrder(Order orderRequest, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.trim().isEmpty()) {
            throw new IllegalArgumentException("Idempotency-Key header is required");
        }

        String redisKey = "idempotency:order:" + idempotencyKey;

        // 1. Check for duplicate key in Redis
        if (redisTemplate != null) {
            try {
                String existingVal = redisTemplate.opsForValue().get(redisKey);
                if (existingVal != null) {
                    log.warn("Duplicate request detected in Redis for key: {}", idempotencyKey);
                    return getDuplicateOrder(idempotencyKey, existingVal);
                }
                // Pre-claim the key
                redisTemplate.opsForValue().set(redisKey, "PENDING", 24, TimeUnit.HOURS);
            } catch (IdempotencyException e) {
                throw e;
            } catch (Exception e) {
                log.warn("Redis unavailable during idempotency check: {}", e.getMessage());
            }
        }

        validateOrder(orderRequest);

        Order order = Order.builder()
                .id(UUID.randomUUID().toString())
                .idempotencyKey(idempotencyKey)
                .symbol(orderRequest.getSymbol().toUpperCase())
                .side(orderRequest.getSide())
                .type(orderRequest.getType())
                .price(orderRequest.getPrice())
                .quantity(orderRequest.getQuantity())
                .status(OrderStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Order savedOrder;
        try {
            savedOrder = orderRepository.saveAndFlush(order);
            log.info("Placed order: {}", savedOrder.getId());
        } catch (DataIntegrityViolationException e) {
            log.warn("Database unique constraint conflict for idempotency key: {}", idempotencyKey);
            // Database-level race condition backstop fallback
            return orderRepository.findByIdempotencyKey(idempotencyKey)
                    .orElseThrow(() -> new IdempotencyException("Duplicate order conflict, but unable to retrieve existing record."));
        }

        // 2. Update idempotency key status in Redis
        if (redisTemplate != null) {
            try {
                redisTemplate.opsForValue().set(redisKey, "SUCCESS:" + savedOrder.getId(), 24, TimeUnit.HOURS);
            } catch (Exception e) {
                log.warn("Failed to update Redis idempotency key status: {}", e.getMessage());
            }
        }

        // Write Audit Log entry
        AuditLog auditLog = AuditLog.builder()
                .eventType("ORDER_PLACED")
                .description("Order placed for symbol " + savedOrder.getSymbol() + ", side: " + savedOrder.getSide() + ", qty: " + savedOrder.getQuantity())
                .build();
        auditLogRepository.save(auditLog);

        // Publish ORDER_PLACED event
        eventPublisher.publishOrderEvent("ORDER_PLACED", savedOrder);

        // Try to execute immediately (especially for MARKET orders, or if LIMIT price already matches)
        executionEngine.processOrder(savedOrder);

        return orderRepository.findById(savedOrder.getId()).orElse(savedOrder);
    }

    @Transactional
    public Order cancelOrder(String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.PARTIALLY_FILLED) {
            throw new IllegalStateException("Order in status " + order.getStatus() + " cannot be cancelled");
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setUpdatedAt(LocalDateTime.now());
        Order savedOrder = orderRepository.save(order);
        log.info("Cancelled order: {}", orderId);

        // Write Audit Log entry
        AuditLog auditLog = AuditLog.builder()
                .eventType("ORDER_CANCELLED")
                .description("Order cancelled: " + orderId)
                .build();
        auditLogRepository.save(auditLog);

        // Publish ORDER_CANCELLED event
        eventPublisher.publishOrderEvent("ORDER_CANCELLED", savedOrder);

        return savedOrder;
    }

    public Order getOrder(String orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    private void validateOrder(Order order) {
        if (order.getSymbol() == null || order.getSymbol().trim().isEmpty()) {
            throw new IllegalArgumentException("Symbol is required");
        }
        if (order.getSide() == null) {
            throw new IllegalArgumentException("Side (BUY/SELL) is required");
        }
        if (order.getType() == null) {
            throw new IllegalArgumentException("Type (LIMIT/MARKET) is required");
        }
        if (order.getQuantity() == null || order.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }
        if (order.getType() == OrderType.LIMIT) {
            if (order.getPrice() == null || order.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Price is required and must be greater than zero for LIMIT orders");
            }
        }
    }

    private Order getDuplicateOrder(String idempotencyKey, String value) {
        if (value != null && value.startsWith("SUCCESS:")) {
            String orderId = value.substring(8);
            return orderRepository.findById(orderId)
                    .orElseGet(() -> orderRepository.findByIdempotencyKey(idempotencyKey)
                            .orElseThrow(() -> new IdempotencyException("Order not found for idempotency key: " + idempotencyKey)));
        }
        return orderRepository.findByIdempotencyKey(idempotencyKey)
                .orElseThrow(() -> new IdempotencyException("Order is currently pending or conflicting."));
    }

    @Transactional
    public String runSettlement() {
        String reconcileSql = "WITH trade_sums AS ( " +
                "    SELECT " +
                "        o.symbol as symbol, " +
                "        SUM(CASE WHEN o.side = 'BUY' THEN t.quantity ELSE -t.quantity END) as calculated_qty " +
                "    FROM trades t " +
                "    JOIN orders o ON t.order_id = o.id " +
                "    GROUP BY o.symbol " +
                "), " +
                "mismatches AS ( " +
                "    SELECT " +
                "        COALESCE(ts.symbol, p.symbol) AS symbol, " +
                "        COALESCE(ts.calculated_qty, 0.0) AS calculated_qty, " +
                "        COALESCE(p.quantity, 0.0) AS position_qty " +
                "    FROM trade_sums ts " +
                "    FULL OUTER JOIN positions p ON ts.symbol = p.symbol " +
                "    WHERE ABS(COALESCE(ts.calculated_qty, 0.0) - COALESCE(p.quantity, 0.0)) > 1e-8 " +
                ") " +
                "SELECT symbol, calculated_qty, position_qty FROM mismatches";

        List<Object[]> mismatches = entityManager.createNativeQuery(reconcileSql).getResultList();
        
        String status = "SUCCESS";
        String description = "Position reconciliation completed successfully. Zero discrepancies found.";
        
        if (!mismatches.isEmpty()) {
            status = "FAILED";
            StringBuilder sb = new StringBuilder("Position reconciliation failed! Mismatches found:\\n");
            for (Object[] row : mismatches) {
                sb.append(String.format("Symbol: %s | Expected Qty: %s | Actual Position Qty: %s\\n",
                        row[0], row[1], row[2]));
            }
            description = sb.toString();
        }
        
        AuditLog auditLog = AuditLog.builder()
                .eventType("EOD_SETTLEMENT_" + status)
                .description(description)
                .createdAt(LocalDateTime.now())
                .build();
        auditLogRepository.save(auditLog);
        
        return "Reconciliation Status: " + status + "\n" + description;
    }
}
