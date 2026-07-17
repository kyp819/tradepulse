package com.tradepulse.priceingestion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradepulse.priceingestion.event.PriceEvent;
import com.tradepulse.priceingestion.service.PricePublisher;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ActiveProfiles("test")
@SpringBootTest
@Testcontainers
public class PricePublisherIT {

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
    private PricePublisher pricePublisher;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private Consumer<String, String> consumer;

    @BeforeEach
    void setUp() {
        // Clear Redis cache before test
        try {
            redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
        } catch (Exception e) {
            // Ignore
        }

        // Setup Kafka Consumer to verify message sent
        Map<String, Object> consumerProps = new HashMap<>();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "test-group");
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        
        DefaultKafkaConsumerFactory<String, String> cf = new DefaultKafkaConsumerFactory<>(consumerProps);
        consumer = cf.createConsumer();
        consumer.subscribe(Collections.singletonList("prices"));
    }

    @AfterEach
    void tearDown() {
        if (consumer != null) {
            consumer.close();
        }
    }

    @Test
    public void testProcessAndPublish() throws Exception {
        String mockPayload = "{\"e\":\"24hrTicker\",\"E\":1672531200000,\"s\":\"BTCUSDT\",\"c\":\"16800.50\"}";

        pricePublisher.processAndPublish(mockPayload);

        // Verify key updated in real Redis
        String cachedPrice = redisTemplate.opsForValue().get("price:BTCUSDT");
        assertNotNull(cachedPrice);
        assertEquals("16800.50", cachedPrice);

        // Verify message received in real Kafka
        ConsumerRecord<String, String> record = KafkaTestUtils.getSingleRecord(consumer, "prices", java.time.Duration.ofMillis(5000));
        assertNotNull(record);
        assertEquals("BTCUSDT", record.key());

        PriceEvent priceEvent = objectMapper.readValue(record.value(), PriceEvent.class);
        assertEquals("BTCUSDT", priceEvent.getSymbol());
        assertEquals(0, BigDecimal.valueOf(16800.50).compareTo(priceEvent.getPrice()));
        assertEquals(1672531200000L, priceEvent.getTimestamp());
    }
}
