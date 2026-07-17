package com.tradepulse.orderservice;

import com.tradepulse.orderservice.model.Order;
import com.tradepulse.orderservice.model.OrderSide;
import com.tradepulse.orderservice.model.OrderType;
import com.tradepulse.orderservice.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.*;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
class IdempotencyIT {

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
    private TestRestTemplate restTemplate;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        try {
            redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
        } catch (Exception e) {
            // ignore
        }
    }

    @Test
    void testIdempotencyFlow() {
        String key = "test-idempotency-key-uuid";
        Order orderRequest = Order.builder()
                .symbol("BTCUSDT")
                .side(OrderSide.BUY)
                .type(OrderType.MARKET)
                .quantity(BigDecimal.valueOf(1.5))
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Idempotency-Key", key);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Order> entity = new HttpEntity<>(orderRequest, headers);

        // 1st POST -> creates order
        ResponseEntity<Order> response1 = restTemplate.postForEntity("/api/orders", entity, Order.class);
        assertEquals(HttpStatus.OK, response1.getStatusCode());
        Order firstOrder = response1.getBody();
        assertNotNull(firstOrder);
        assertNotNull(firstOrder.getId());

        // 2nd POST -> returns 200 OK with same order id
        ResponseEntity<Order> response2 = restTemplate.postForEntity("/api/orders", entity, Order.class);
        assertEquals(HttpStatus.OK, response2.getStatusCode());
        Order secondOrder = response2.getBody();
        assertNotNull(secondOrder);
        assertEquals(firstOrder.getId(), secondOrder.getId());

        // 3. Clear Redis key manually
        redisTemplate.delete("idempotency:order:" + key);

        // 4. 3rd POST -> database constraint should trigger catch block in OrderService and return original order
        ResponseEntity<Order> response3 = restTemplate.postForEntity("/api/orders", entity, Order.class);
        assertEquals(HttpStatus.OK, response3.getStatusCode());
        Order thirdOrder = response3.getBody();
        assertNotNull(thirdOrder);
        assertEquals(firstOrder.getId(), thirdOrder.getId());

        // Assert only 1 order exists in database
        assertEquals(1, orderRepository.count());
    }
}
