package com.tradepulse.orderservice;

import com.tradepulse.orderservice.exception.IdempotencyException;
import com.tradepulse.orderservice.exception.ResourceNotFoundException;
import com.tradepulse.orderservice.model.*;
import com.tradepulse.orderservice.repository.AuditLogRepository;
import com.tradepulse.orderservice.repository.OrderRepository;
import com.tradepulse.orderservice.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OrderServiceTest {

    private OrderService orderService;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PriceCacheService priceCacheService;

    @Mock
    private OrderEventPublisher eventPublisher;

    @Mock
    private OrderExecutionEngine executionEngine;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        orderService = new OrderService(orderRepository, priceCacheService, eventPublisher, executionEngine, auditLogRepository, redisTemplate);
    }

    @Test
    void testPlaceMarketOrder_Success() {
        Order request = Order.builder()
                .symbol("BTCUSDT")
                .side(OrderSide.BUY)
                .type(OrderType.MARKET)
                .quantity(BigDecimal.ONE)
                .build();

        Order saved = Order.builder()
                .id("123")
                .symbol("BTCUSDT")
                .side(OrderSide.BUY)
                .type(OrderType.MARKET)
                .quantity(BigDecimal.ONE)
                .status(OrderStatus.PENDING)
                .build();

        when(orderRepository.saveAndFlush(any(Order.class))).thenReturn(saved);
        when(orderRepository.findById("123")).thenReturn(Optional.of(saved));
        when(valueOperations.get(anyString())).thenReturn(null);

        Order result = orderService.placeOrder(request, "test-idempotency-key");

        assertNotNull(result);
        assertEquals("BTCUSDT", result.getSymbol());
        verify(orderRepository).saveAndFlush(any(Order.class));
        verify(eventPublisher).publishOrderEvent(eq("ORDER_PLACED"), any(Order.class));
        verify(executionEngine).processOrder(any(Order.class));
        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    void testPlaceOrder_ValidationFailure() {
        Order request = Order.builder()
                .symbol("") // invalid
                .side(OrderSide.BUY)
                .type(OrderType.MARKET)
                .quantity(BigDecimal.ONE)
                .build();

        assertThrows(IllegalArgumentException.class, () -> orderService.placeOrder(request, "test-idempotency-key"));
    }

    @Test
    void testPlaceOrder_DuplicateIdempotencyKey() {
        Order request = Order.builder()
                .symbol("BTCUSDT")
                .side(OrderSide.BUY)
                .type(OrderType.MARKET)
                .quantity(BigDecimal.ONE)
                .build();

        when(valueOperations.get("idempotency:order:duplicate-key")).thenReturn("PENDING");
        when(orderRepository.findByIdempotencyKey("duplicate-key")).thenReturn(Optional.empty());

        assertThrows(IdempotencyException.class, () -> orderService.placeOrder(request, "duplicate-key"));
    }

    @Test
    void testCancelOrder_Success() {
        Order pendingOrder = Order.builder()
                .id("123")
                .symbol("BTCUSDT")
                .side(OrderSide.BUY)
                .type(OrderType.LIMIT)
                .price(BigDecimal.valueOf(50000))
                .quantity(BigDecimal.ONE)
                .status(OrderStatus.PENDING)
                .build();

        when(orderRepository.findById("123")).thenReturn(Optional.of(pendingOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Order result = orderService.cancelOrder("123");

        assertEquals(OrderStatus.CANCELLED, result.getStatus());
        verify(eventPublisher).publishOrderEvent(eq("ORDER_CANCELLED"), any(Order.class));
        verify(auditLogRepository).save(any(AuditLog.class));
    }


    @Test
    void testCancelOrder_NotFound() {
        when(orderRepository.findById("non-existent")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> orderService.cancelOrder("non-existent"));
    }

    @Test
    void testCancelOrder_AlreadyFilled_throws() {
        Order filledOrder = Order.builder()
                .id("456")
                .symbol("BTCUSDT")
                .side(OrderSide.BUY)
                .type(OrderType.MARKET)
                .quantity(BigDecimal.ONE)
                .status(OrderStatus.FILLED)
                .build();

        when(orderRepository.findById("456")).thenReturn(Optional.of(filledOrder));

        assertThrows(IllegalStateException.class, () -> orderService.cancelOrder("456"));
    }

    @Test
    void testGetOrder_found() {
        Order order = Order.builder().id("789").symbol("ETHUSDT").status(OrderStatus.PENDING).build();
        when(orderRepository.findById("789")).thenReturn(Optional.of(order));

        Order result = orderService.getOrder("789");

        assertEquals("789", result.getId());
    }

    @Test
    void testGetOrder_notFound_throws() {
        when(orderRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> orderService.getOrder("missing"));
    }

    @Test
    void testGetAllOrders_returnsList() {
        Order o1 = Order.builder().id("1").symbol("BTCUSDT").status(OrderStatus.PENDING).build();
        Order o2 = Order.builder().id("2").symbol("ETHUSDT").status(OrderStatus.FILLED).build();
        when(orderRepository.findAll()).thenReturn(java.util.List.of(o1, o2));

        java.util.List<Order> result = orderService.getAllOrders();

        assertEquals(2, result.size());
    }

    @Test
    void testPlaceOrder_nullIdempotencyKey_throws() {
        Order request = Order.builder()
                .symbol("BTCUSDT")
                .side(OrderSide.BUY)
                .type(OrderType.MARKET)
                .quantity(BigDecimal.ONE)
                .build();

        assertThrows(IllegalArgumentException.class, () -> orderService.placeOrder(request, null));
    }

    @Test
    void testPlaceOrder_limitOrder_missingPrice_throws() {
        Order request = Order.builder()
                .symbol("BTCUSDT")
                .side(OrderSide.BUY)
                .type(OrderType.LIMIT)
                .quantity(BigDecimal.ONE)
                .price(null)   // missing required price for LIMIT
                .build();

        when(valueOperations.get(anyString())).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () -> orderService.placeOrder(request, "limit-key"));
    }

    @Test
    void testPlaceOrder_duplicateKey_successBranch_returnsExistingOrder() {
        Order request = Order.builder()
                .symbol("BTCUSDT")
                .side(OrderSide.BUY)
                .type(OrderType.MARKET)
                .quantity(BigDecimal.ONE)
                .build();

        Order existing = Order.builder().id("existing-id").symbol("BTCUSDT").status(OrderStatus.PENDING).build();
        when(valueOperations.get("idempotency:order:success-key")).thenReturn("SUCCESS:existing-id");
        when(orderRepository.findById("existing-id")).thenReturn(Optional.of(existing));

        Order result = orderService.placeOrder(request, "success-key");

        assertEquals("existing-id", result.getId());
    }
}

