package com.tradepulse.orderservice;

import com.tradepulse.orderservice.model.*;
import com.tradepulse.orderservice.repository.OrderRepository;
import com.tradepulse.orderservice.repository.PositionRepository;
import com.tradepulse.orderservice.repository.TradeRepository;
import com.tradepulse.orderservice.service.OrderExecutionEngine;
import com.tradepulse.orderservice.service.PriceCacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class ConcurrentFillIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("tradepulse")
            .withUsername("postgres")
            .withPassword("postgres");

    @Container
    @ServiceConnection
    static final KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.0"));

    @Container
    static final GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @Autowired
    private OrderExecutionEngine executionEngine;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PositionRepository positionRepository;

    @Autowired
    private TradeRepository tradeRepository;

    @Autowired
    private PriceCacheService priceCacheService;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        tradeRepository.deleteAll();
        positionRepository.deleteAll();
    }

    @Test
    void testConcurrentFillsPessimisticLock() throws Exception {
        String symbol = "BTCUSDT";
        priceCacheService.updatePrice(symbol, BigDecimal.valueOf(50000));

        // Create one initial Position
        Position initialPosition = Position.builder()
                .symbol(symbol)
                .quantity(BigDecimal.ZERO)
                .averagePrice(BigDecimal.ZERO)
                .updatedAt(LocalDateTime.now())
                .build();
        positionRepository.saveAndFlush(initialPosition);

        // Pre-create two pending market orders
        Order order1 = Order.builder()
                .id(UUID.randomUUID().toString())
                .symbol(symbol)
                .side(OrderSide.BUY)
                .type(OrderType.MARKET)
                .quantity(BigDecimal.valueOf(1.0))
                .status(OrderStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Order order2 = Order.builder()
                .id(UUID.randomUUID().toString())
                .symbol(symbol)
                .side(OrderSide.BUY)
                .type(OrderType.MARKET)
                .quantity(BigDecimal.valueOf(2.0))
                .status(OrderStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        orderRepository.saveAndFlush(order1);
        orderRepository.saveAndFlush(order2);

        // Execute concurrent fills using CountDownLatch
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);

        executor.submit(() -> {
            try {
                startLatch.await();
                executionEngine.processOrder(order1);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                doneLatch.countDown();
            }
        });

        executor.submit(() -> {
            try {
                startLatch.await();
                executionEngine.processOrder(order2);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                doneLatch.countDown();
            }
        });

        // Trigger simultaneous start
        startLatch.countDown();
        assertTrue(doneLatch.await(10, TimeUnit.SECONDS));

        executor.shutdown();

        // Verify Position state
        Position finalPosition = positionRepository.findById(symbol).orElseThrow();
        assertEquals(0, BigDecimal.valueOf(3.0).compareTo(finalPosition.getQuantity()), "Quantity should equal sum of both fills");
        assertEquals(0, BigDecimal.valueOf(50000).compareTo(finalPosition.getAveragePrice()), "Average price should be correct");
    }
}
