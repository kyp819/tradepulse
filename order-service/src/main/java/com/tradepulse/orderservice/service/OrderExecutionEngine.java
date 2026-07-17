package com.tradepulse.orderservice.service;

import com.tradepulse.orderservice.model.*;
import com.tradepulse.orderservice.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class OrderExecutionEngine {

    private final OrderRepository orderRepository;
    private final TradeRepository tradeRepository;
    private final PositionRepository positionRepository;
    private final AuditLogRepository auditLogRepository;
    private final PriceCacheService priceCacheService;
    private final OrderEventPublisher eventPublisher;

    @Autowired
    public OrderExecutionEngine(OrderRepository orderRepository,
                                TradeRepository tradeRepository,
                                PositionRepository positionRepository,
                                AuditLogRepository auditLogRepository,
                                PriceCacheService priceCacheService,
                                @Lazy OrderEventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.tradeRepository = tradeRepository;
        this.positionRepository = positionRepository;
        this.auditLogRepository = auditLogRepository;
        this.priceCacheService = priceCacheService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void processOrder(Order order) {
        if (order.getStatus() != OrderStatus.PENDING) {
            return;
        }

        BigDecimal marketPrice = priceCacheService.getLatestPrice(order.getSymbol());
        if (marketPrice == null) {
            log.info("No current market price for {}. Order {} remains PENDING.", order.getSymbol(), order.getId());
            return;
        }

        boolean shouldExecute = false;
        BigDecimal executionPrice = marketPrice;

        if (order.getType() == OrderType.MARKET) {
            shouldExecute = true;
        } else if (order.getType() == OrderType.LIMIT) {
            if (order.getSide() == OrderSide.BUY && marketPrice.compareTo(order.getPrice()) <= 0) {
                shouldExecute = true;
                executionPrice = marketPrice; // or fill at market/limit price, usually the better of two or limit price itself. In simple matching, we fill at current price.
            } else if (order.getSide() == OrderSide.SELL && marketPrice.compareTo(order.getPrice()) >= 0) {
                shouldExecute = true;
                executionPrice = marketPrice;
            }
        }

        if (shouldExecute) {
            executeFill(order, executionPrice);
        }
    }

    @Transactional
    public void matchPendingOrders(String symbol, BigDecimal marketPrice) {
        List<Order> pendingOrders = orderRepository.findByStatus(OrderStatus.PENDING);
        for (Order order : pendingOrders) {
            if (order.getSymbol().equalsIgnoreCase(symbol)) {
                boolean shouldExecute = false;
                if (order.getType() == OrderType.MARKET) {
                    shouldExecute = true;
                } else if (order.getType() == OrderType.LIMIT) {
                    if (order.getSide() == OrderSide.BUY && marketPrice.compareTo(order.getPrice()) <= 0) {
                        shouldExecute = true;
                    } else if (order.getSide() == OrderSide.SELL && marketPrice.compareTo(order.getPrice()) >= 0) {
                        shouldExecute = true;
                    }
                }

                if (shouldExecute) {
                    try {
                        executeFill(order, marketPrice);
                    } catch (Exception e) {
                        log.error("Failed to execute order {}: {}", order.getId(), e.getMessage());
                    }
                }
            }
        }
    }

    private void executeFill(Order order, BigDecimal executionPrice) {
        log.info("Executing fill for Order {} at price {}", order.getId(), executionPrice);

        // 1. Update Order status
        order.setStatus(OrderStatus.FILLED);
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);

        // 2. Create Trade execution record
        Trade trade = Trade.builder()
                .id(UUID.randomUUID().toString())
                .orderId(order.getId())
                .price(executionPrice)
                .quantity(order.getQuantity())
                .executedAt(LocalDateTime.now())
                .build();
        tradeRepository.save(trade);

        // 3. Update Position
        Position position = updatePosition(order.getSymbol(), order.getSide(), order.getQuantity(), executionPrice);

        // 4. Log Audit Entry
        AuditLog auditLog = AuditLog.builder()
                .eventType("ORDER_FILLED")
                .description(String.format("Order %s (%s %s %s) filled at %s. New Position: Qty=%s, AvgPrice=%s",
                        order.getId(), order.getSide(), order.getQuantity(), order.getSymbol(), executionPrice,
                        position.getQuantity(), position.getAveragePrice()))
                .createdAt(LocalDateTime.now())
                .build();
        auditLogRepository.save(auditLog);

        // 5. Publish event to Kafka
        eventPublisher.publishOrderEvent("ORDER_FILLED", order);
    }

    private Position updatePosition(String symbol, OrderSide side, BigDecimal fillQty, BigDecimal fillPrice) {
        BigDecimal tradeQty = side == OrderSide.BUY ? fillQty : fillQty.negate();
        Position position = positionRepository.findBySymbolWithLock(symbol)
                .orElse(Position.builder()
                        .symbol(symbol)
                        .quantity(BigDecimal.ZERO)
                        .averagePrice(BigDecimal.ZERO)
                        .updatedAt(LocalDateTime.now())
                        .build());

        BigDecimal oldQty = position.getQuantity();
        BigDecimal oldAvgPrice = position.getAveragePrice();
        BigDecimal newQty = oldQty.add(tradeQty);

        if (newQty.compareTo(BigDecimal.ZERO) == 0) {
            position.setQuantity(BigDecimal.ZERO);
            position.setAveragePrice(BigDecimal.ZERO);
        } else if (oldQty.compareTo(BigDecimal.ZERO) == 0) {
            position.setQuantity(newQty);
            position.setAveragePrice(fillPrice);
        } else {
            boolean adding = (oldQty.signum() == tradeQty.signum());
            if (adding) {
                BigDecimal oldCost = oldQty.multiply(oldAvgPrice).abs();
                BigDecimal tradeCost = tradeQty.multiply(fillPrice).abs();
                BigDecimal newCost = oldCost.add(tradeCost);
                BigDecimal newAvgPrice = newCost.divide(newQty.abs(), 8, java.math.RoundingMode.HALF_UP);
                position.setAveragePrice(newAvgPrice);
                position.setQuantity(newQty);
            } else {
                if (oldQty.abs().compareTo(tradeQty.abs()) >= 0) {
                    position.setQuantity(newQty);
                } else {
                    position.setQuantity(newQty);
                    position.setAveragePrice(fillPrice);
                }
            }
        }
        position.setUpdatedAt(LocalDateTime.now());
        return positionRepository.save(position);
    }
}
