package com.tradepulse.orderservice;

import com.tradepulse.orderservice.model.*;
import com.tradepulse.orderservice.repository.OrderRepository;
import com.tradepulse.orderservice.repository.PositionRepository;
import com.tradepulse.orderservice.repository.TradeRepository;
import jakarta.persistence.EntityManager;
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
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class ReconciliationIT {

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
    private OrderRepository orderRepository;

    @Autowired
    private TradeRepository tradeRepository;

    @Autowired
    private PositionRepository positionRepository;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void setUp() {
        tradeRepository.deleteAll();
        orderRepository.deleteAll();
        positionRepository.deleteAll();
    }

    @Test
    void testReconciliationQueryLogic() {
        String symbol = "BTCUSDT";

        // 1. Create a trade of 5.0 Qty
        Order order = Order.builder()
                .id(UUID.randomUUID().toString())
                .symbol(symbol)
                .side(OrderSide.BUY)
                .type(OrderType.MARKET)
                .quantity(BigDecimal.valueOf(5.0))
                .status(OrderStatus.FILLED)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        orderRepository.saveAndFlush(order);

        Trade trade = Trade.builder()
                .id(UUID.randomUUID().toString())
                .orderId(order.getId())
                .price(BigDecimal.valueOf(50000))
                .quantity(BigDecimal.valueOf(5.0))
                .executedAt(LocalDateTime.now())
                .build();
        tradeRepository.saveAndFlush(trade);

        // 2. Create a mismatched position of 4.0 Qty
        Position position = Position.builder()
                .symbol(symbol)
                .quantity(BigDecimal.valueOf(4.0)) // mismatched: 4.0 != 5.0
                .averagePrice(BigDecimal.valueOf(50000))
                .updatedAt(LocalDateTime.now())
                .build();
        positionRepository.saveAndFlush(position);

        // 3. Run reconciliation query logic
        String sql = "WITH trade_sums AS (" +
                "    SELECT " +
                "        o.symbol, " +
                "        SUM(CASE WHEN o.side = 'BUY' THEN t.quantity ELSE -t.quantity END) as calculated_qty " +
                "    FROM trades t " +
                "    JOIN orders o ON t.order_id = o.id " +
                "    GROUP BY o.symbol " +
                "), " +
                "mismatches AS (" +
                "    SELECT " +
                "        COALESCE(ts.symbol, p.symbol) AS symbol, " +
                "        COALESCE(ts.calculated_qty, 0.0) AS calculated_qty, " +
                "        COALESCE(p.quantity, 0.0) AS position_qty " +
                "    FROM trade_sums ts " +
                "    FULL OUTER JOIN positions p ON ts.symbol = p.symbol " +
                "    WHERE ABS(COALESCE(ts.calculated_qty, 0.0) - COALESCE(p.quantity, 0.0)) > 1e-8 " +
                ") " +
                "SELECT symbol, calculated_qty, position_qty FROM mismatches";

        List<?> results = entityManager.createNativeQuery(sql).getResultList();
        assertEquals(1, results.size(), "Discrepancy should be detected");

        Object[] row = (Object[]) results.get(0);
        assertEquals(symbol, row[0]);
        assertEquals(0, BigDecimal.valueOf(5.0).compareTo((BigDecimal) row[1]), "Calculated quantity matches trade sum");
        assertEquals(0, BigDecimal.valueOf(4.0).compareTo((BigDecimal) row[2]), "Position quantity matches incorrect position record");

        // 4. Resolve the mismatch by correcting the position to 5.0 Qty
        position.setQuantity(BigDecimal.valueOf(5.0));
        positionRepository.saveAndFlush(position);

        // 5. Re-run reconciliation query
        List<?> noResults = entityManager.createNativeQuery(sql).getResultList();
        assertTrue(noResults.isEmpty(), "No discrepancies should remain when values match");
    }
}
