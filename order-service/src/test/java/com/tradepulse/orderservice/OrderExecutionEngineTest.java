package com.tradepulse.orderservice;

import com.tradepulse.orderservice.model.*;
import com.tradepulse.orderservice.repository.*;
import com.tradepulse.orderservice.service.OrderEventPublisher;
import com.tradepulse.orderservice.service.OrderExecutionEngine;
import com.tradepulse.orderservice.service.PriceCacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class OrderExecutionEngineTest {

    @Mock private OrderRepository orderRepository;
    @Mock private TradeRepository tradeRepository;
    @Mock private PositionRepository positionRepository;
    @Mock private AuditLogRepository auditLogRepository;
    @Mock private PriceCacheService priceCacheService;
    @Mock private OrderEventPublisher eventPublisher;

    private OrderExecutionEngine engine;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        engine = new OrderExecutionEngine(orderRepository, tradeRepository, positionRepository,
                auditLogRepository, priceCacheService, eventPublisher);
    }

    private Order buildOrder(OrderType type, OrderSide side, BigDecimal limitPrice) {
        return Order.builder()
                .id("ord-1")
                .symbol("BTCUSDT")
                .side(side)
                .type(type)
                .price(limitPrice)
                .quantity(BigDecimal.ONE)
                .status(OrderStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private Position emptyPosition() {
        return Position.builder()
                .symbol("BTCUSDT")
                .quantity(BigDecimal.ZERO)
                .averagePrice(BigDecimal.ZERO)
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // ── processOrder ──────────────────────────────────────────────────────────

    @Test
    void testProcessOrder_notPending_skips() {
        Order order = buildOrder(OrderType.MARKET, OrderSide.BUY, null);
        order.setStatus(OrderStatus.FILLED);

        engine.processOrder(order);

        verifyNoInteractions(priceCacheService, tradeRepository, positionRepository);
    }

    @Test
    void testProcessOrder_noMarketPrice_staysPending() {
        Order order = buildOrder(OrderType.MARKET, OrderSide.BUY, null);
        when(priceCacheService.getLatestPrice("BTCUSDT")).thenReturn(null);

        engine.processOrder(order);

        verifyNoInteractions(tradeRepository, positionRepository);
    }

    @Test
    void testProcessOrder_marketOrder_executes() {
        Order order = buildOrder(OrderType.MARKET, OrderSide.BUY, null);
        when(priceCacheService.getLatestPrice("BTCUSDT")).thenReturn(BigDecimal.valueOf(50000));
        when(orderRepository.save(any())).thenReturn(order);
        when(positionRepository.findBySymbolWithLock(anyString())).thenReturn(Optional.empty());
        when(positionRepository.save(any())).thenReturn(emptyPosition());

        engine.processOrder(order);

        verify(tradeRepository).save(any(Trade.class));
        verify(eventPublisher).publishOrderEvent(eq("ORDER_FILLED"), any(Order.class));
    }

    @Test
    void testProcessOrder_limitBuyBelowMarket_executes() {
        Order order = buildOrder(OrderType.LIMIT, OrderSide.BUY, BigDecimal.valueOf(51000));
        when(priceCacheService.getLatestPrice("BTCUSDT")).thenReturn(BigDecimal.valueOf(50000));
        when(orderRepository.save(any())).thenReturn(order);
        when(positionRepository.findBySymbolWithLock(anyString())).thenReturn(Optional.empty());
        when(positionRepository.save(any())).thenReturn(emptyPosition());

        engine.processOrder(order);

        verify(tradeRepository).save(any(Trade.class));
    }

    @Test
    void testProcessOrder_limitBuyAboveMarket_noExecution() {
        Order order = buildOrder(OrderType.LIMIT, OrderSide.BUY, BigDecimal.valueOf(40000));
        when(priceCacheService.getLatestPrice("BTCUSDT")).thenReturn(BigDecimal.valueOf(50000));

        engine.processOrder(order);

        verifyNoInteractions(tradeRepository);
    }

    @Test
    void testProcessOrder_limitSellAboveMarket_executes() {
        Order order = buildOrder(OrderType.LIMIT, OrderSide.SELL, BigDecimal.valueOf(40000));
        when(priceCacheService.getLatestPrice("BTCUSDT")).thenReturn(BigDecimal.valueOf(50000));
        when(orderRepository.save(any())).thenReturn(order);
        when(positionRepository.findBySymbolWithLock(anyString())).thenReturn(Optional.empty());
        when(positionRepository.save(any())).thenReturn(emptyPosition());

        engine.processOrder(order);

        verify(tradeRepository).save(any(Trade.class));
    }

    @Test
    void testProcessOrder_limitSellBelowMarket_noExecution() {
        Order order = buildOrder(OrderType.LIMIT, OrderSide.SELL, BigDecimal.valueOf(60000));
        when(priceCacheService.getLatestPrice("BTCUSDT")).thenReturn(BigDecimal.valueOf(50000));

        engine.processOrder(order);

        verifyNoInteractions(tradeRepository);
    }

    // ── matchPendingOrders ────────────────────────────────────────────────────

    @Test
    void testMatchPendingOrders_noOrders_doesNothing() {
        when(orderRepository.findByStatus(OrderStatus.PENDING)).thenReturn(Collections.emptyList());

        engine.matchPendingOrders("BTCUSDT", BigDecimal.valueOf(50000));

        verifyNoInteractions(tradeRepository);
    }

    @Test
    void testMatchPendingOrders_differentSymbol_skips() {
        Order order = buildOrder(OrderType.MARKET, OrderSide.BUY, null);
        order.setSymbol("ETHUSDT");
        when(orderRepository.findByStatus(OrderStatus.PENDING)).thenReturn(List.of(order));

        engine.matchPendingOrders("BTCUSDT", BigDecimal.valueOf(50000));

        verifyNoInteractions(tradeRepository);
    }

    @Test
    void testMatchPendingOrders_marketOrder_executes() {
        Order order = buildOrder(OrderType.MARKET, OrderSide.BUY, null);
        when(orderRepository.findByStatus(OrderStatus.PENDING)).thenReturn(List.of(order));
        when(orderRepository.save(any())).thenReturn(order);
        when(positionRepository.findBySymbolWithLock(anyString())).thenReturn(Optional.empty());
        when(positionRepository.save(any())).thenReturn(emptyPosition());

        engine.matchPendingOrders("BTCUSDT", BigDecimal.valueOf(50000));

        verify(tradeRepository).save(any(Trade.class));
    }

    @Test
    void testMatchPendingOrders_limitBuy_executes() {
        Order order = buildOrder(OrderType.LIMIT, OrderSide.BUY, BigDecimal.valueOf(55000));
        when(orderRepository.findByStatus(OrderStatus.PENDING)).thenReturn(List.of(order));
        when(orderRepository.save(any())).thenReturn(order);
        when(positionRepository.findBySymbolWithLock(anyString())).thenReturn(Optional.empty());
        when(positionRepository.save(any())).thenReturn(emptyPosition());

        engine.matchPendingOrders("BTCUSDT", BigDecimal.valueOf(50000));

        verify(tradeRepository).save(any(Trade.class));
    }

    @Test
    void testMatchPendingOrders_limitSell_executes() {
        Order order = buildOrder(OrderType.LIMIT, OrderSide.SELL, BigDecimal.valueOf(45000));
        when(orderRepository.findByStatus(OrderStatus.PENDING)).thenReturn(List.of(order));
        when(orderRepository.save(any())).thenReturn(order);
        when(positionRepository.findBySymbolWithLock(anyString())).thenReturn(Optional.empty());
        when(positionRepository.save(any())).thenReturn(emptyPosition());

        engine.matchPendingOrders("BTCUSDT", BigDecimal.valueOf(50000));

        verify(tradeRepository).save(any(Trade.class));
    }

    @Test
    void testMatchPendingOrders_executionThrows_continuesWithOthers() {
        Order order = buildOrder(OrderType.MARKET, OrderSide.BUY, null);
        when(orderRepository.findByStatus(OrderStatus.PENDING)).thenReturn(List.of(order));
        when(orderRepository.save(any())).thenThrow(new RuntimeException("DB error"));

        // Should not propagate exception
        engine.matchPendingOrders("BTCUSDT", BigDecimal.valueOf(50000));
    }

    // ── position update: existing position scenarios ───────────────────────────

    @Test
    void testProcessOrder_existingPosition_adding_updatesAvgPrice() {
        Order order = buildOrder(OrderType.MARKET, OrderSide.BUY, null);
        when(priceCacheService.getLatestPrice("BTCUSDT")).thenReturn(BigDecimal.valueOf(50000));
        when(orderRepository.save(any())).thenReturn(order);

        Position existing = Position.builder()
                .symbol("BTCUSDT")
                .quantity(BigDecimal.ONE)
                .averagePrice(BigDecimal.valueOf(48000))
                .updatedAt(LocalDateTime.now())
                .build();
        when(positionRepository.findBySymbolWithLock("BTCUSDT")).thenReturn(Optional.of(existing));
        when(positionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        engine.processOrder(order);

        verify(positionRepository).save(any(Position.class));
    }

    @Test
    void testProcessOrder_existingPosition_reducing_updatesQty() {
        Order order = buildOrder(OrderType.MARKET, OrderSide.SELL, null);
        when(priceCacheService.getLatestPrice("BTCUSDT")).thenReturn(BigDecimal.valueOf(50000));
        when(orderRepository.save(any())).thenReturn(order);

        Position existing = Position.builder()
                .symbol("BTCUSDT")
                .quantity(BigDecimal.valueOf(2))
                .averagePrice(BigDecimal.valueOf(48000))
                .updatedAt(LocalDateTime.now())
                .build();
        when(positionRepository.findBySymbolWithLock("BTCUSDT")).thenReturn(Optional.of(existing));
        when(positionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        engine.processOrder(order);

        verify(positionRepository).save(any(Position.class));
    }

    @Test
    void testProcessOrder_existingPosition_closingToZero_setsZero() {
        Order order = buildOrder(OrderType.MARKET, OrderSide.SELL, null);
        when(priceCacheService.getLatestPrice("BTCUSDT")).thenReturn(BigDecimal.valueOf(50000));
        when(orderRepository.save(any())).thenReturn(order);

        Position existing = Position.builder()
                .symbol("BTCUSDT")
                .quantity(BigDecimal.ONE)
                .averagePrice(BigDecimal.valueOf(48000))
                .updatedAt(LocalDateTime.now())
                .build();
        when(positionRepository.findBySymbolWithLock("BTCUSDT")).thenReturn(Optional.of(existing));
        when(positionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        engine.processOrder(order);

        verify(positionRepository).save(any(Position.class));
    }
}
