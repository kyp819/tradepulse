package com.tradepulse.orderservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradepulse.orderservice.event.PriceEvent;
import com.tradepulse.orderservice.model.*;
import com.tradepulse.orderservice.repository.OrderRepository;
import com.tradepulse.orderservice.repository.PositionRepository;
import com.tradepulse.orderservice.repository.TradeRepository;
import com.tradepulse.orderservice.service.OrderService;
import com.tradepulse.orderservice.service.PriceCacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
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
import java.util.List;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class OrderIntegrationIT {

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
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private TradeRepository tradeRepository;

    @Autowired
    private PositionRepository positionRepository;

    @Autowired
    private PriceCacheService priceCacheService;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final BlockingQueue<String> dltRecords = new LinkedBlockingQueue<>();

    @org.springframework.kafka.annotation.KafkaListener(topics = "prices.DLT", groupId = "test-dlt-group")
    public void listenDlt(String message) {
        dltRecords.add(message);
    }

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        tradeRepository.deleteAll();
        positionRepository.deleteAll();
        dltRecords.clear();
        try {
            redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
        } catch (Exception e) {
            // ignore if redis flushing fails
        }
    }

    @Test
    void testOrderPlacementAndMatchingFlow() throws Exception {
        // 1. Initial cached price
        priceCacheService.updatePrice("BTCUSDT", BigDecimal.valueOf(61000));

        // 2. Submit Limit Buy Order at 60000 (pending, as latest market price is 61000)
        Order buyOrderRequest = Order.builder()
                .symbol("BTCUSDT")
                .side(OrderSide.BUY)
                .type(OrderType.LIMIT)
                .price(BigDecimal.valueOf(60000))
                .quantity(BigDecimal.valueOf(0.5))
                .build();

        Order placedOrder = orderService.placeOrder(buyOrderRequest, "idempotency-key-1");
        assertNotNull(placedOrder.getId());
        assertEquals(OrderStatus.PENDING, placedOrder.getStatus());

        // Verify database state
        Optional<Order> dbOrder = orderRepository.findById(placedOrder.getId());
        assertTrue(dbOrder.isPresent());
        assertEquals(OrderStatus.PENDING, dbOrder.get().getStatus());

        // 3. Publish a new price tick to Kafka below the limit price (e.g. 59500)
        PriceEvent priceTick = PriceEvent.builder()
                .symbol("BTCUSDT")
                .price(BigDecimal.valueOf(59500))
                .timestamp(System.currentTimeMillis())
                .build();

        // Send and wait
        kafkaTemplate.send("prices", "BTCUSDT", priceTick).get();

        // 4. Poll database to wait for async matching to execute the fill
        Order matchedOrder = null;
        for (int i = 0; i < 80; i++) {
            Thread.sleep(200);
            Optional<Order> checkOrder = orderRepository.findById(placedOrder.getId());
            if (checkOrder.isPresent()) {
                if (checkOrder.get().getStatus() == OrderStatus.FILLED) {
                    matchedOrder = checkOrder.get();
                    break;
                }
            }
        }

        // 5. Verification
        assertNotNull(matchedOrder, "Order should be matched and filled");
        assertEquals(OrderStatus.FILLED, matchedOrder.getStatus());

        // Verify Trade Execution
        List<Trade> trades = tradeRepository.findByOrderId(placedOrder.getId());
        assertEquals(1, trades.size());
        Trade trade = trades.get(0);
        assertEquals(BigDecimal.valueOf(59500).setScale(8), trade.getPrice().setScale(8));
        assertEquals(BigDecimal.valueOf(0.5).setScale(8), trade.getQuantity().setScale(8));

        // Verify Position
        Optional<Position> positionOpt = positionRepository.findById("BTCUSDT");
        assertTrue(positionOpt.isPresent());
        Position position = positionOpt.get();
        assertEquals(BigDecimal.valueOf(0.5).setScale(8), position.getQuantity().setScale(8));
        assertEquals(BigDecimal.valueOf(59500).setScale(8), position.getAveragePrice().setScale(8));
    }

    @Test
    void testDLTProcessingOnPoisonPill() throws Exception {
        // Send a malformed message (poison pill) that cannot be deserialized as PriceEvent
        String poisonMessage = "not-a-valid-json";
        kafkaTemplate.send("prices", "BTCUSDT", poisonMessage).get();

        // Verify the message lands in prices.DLT
        String received = dltRecords.poll(15, TimeUnit.SECONDS);
        assertNotNull(received, "DLT should receive the poison pill message within timeout");
        assertEquals(poisonMessage, received);
    }
}
