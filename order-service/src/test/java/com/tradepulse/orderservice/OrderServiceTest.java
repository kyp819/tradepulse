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
}
