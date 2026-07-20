package com.tradepulse.priceingestion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradepulse.priceingestion.event.PriceEvent;
import com.tradepulse.priceingestion.service.PricePublisher;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PricePublisherUnitTest {

    @Test
    void testProcessAndPublish_InvalidJson() {
        KafkaTemplate<String, Object> kafkaTemplate = mock(KafkaTemplate.class);
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ObjectMapper objectMapper = new ObjectMapper();
        PricePublisher publisher = new PricePublisher(kafkaTemplate, redisTemplate, objectMapper, "prices");

        // This will throw exception in readValue
        publisher.processAndPublish("invalid json");

        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void testProcessAndPublish_MissingFields() {
        KafkaTemplate<String, Object> kafkaTemplate = mock(KafkaTemplate.class);
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ObjectMapper objectMapper = new ObjectMapper();
        PricePublisher publisher = new PricePublisher(kafkaTemplate, redisTemplate, objectMapper, "prices");

        // Missing symbol
        publisher.processAndPublish("{\"c\":\"16800.50\"}");
        // Missing lastPrice
        publisher.processAndPublish("{\"s\":\"BTCUSDT\"}");

        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void testProcessAndPublish_NullRedisTemplate() {
        KafkaTemplate<String, Object> kafkaTemplate = mock(KafkaTemplate.class);
        ObjectMapper objectMapper = new ObjectMapper();
        PricePublisher publisher = new PricePublisher(kafkaTemplate, null, objectMapper, "prices");

        String payload = "{\"s\":\"BTCUSDT\",\"c\":\"16800.50\"}";
        publisher.processAndPublish(payload);

        verify(kafkaTemplate).send(eq("prices"), eq("BTCUSDT"), any(PriceEvent.class));
    }

    @Test
    void testProcessAndPublish_RedisThrowsException() {
        KafkaTemplate<String, Object> kafkaTemplate = mock(KafkaTemplate.class);
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        doThrow(new RuntimeException("Redis down")).when(valueOperations).set(anyString(), anyString());

        ObjectMapper objectMapper = new ObjectMapper();
        PricePublisher publisher = new PricePublisher(kafkaTemplate, redisTemplate, objectMapper, "prices");

        String payload = "{\"s\":\"BTCUSDT\",\"c\":\"16800.50\"}";
        // Should not throw exception, should log and continue
        publisher.processAndPublish(payload);

        verify(kafkaTemplate).send(eq("prices"), eq("BTCUSDT"), any(PriceEvent.class));
    }

    @Test
    void testProcessAndPublish_NullEventTime() {
        KafkaTemplate<String, Object> kafkaTemplate = mock(KafkaTemplate.class);
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        ObjectMapper objectMapper = new ObjectMapper();
        PricePublisher publisher = new PricePublisher(kafkaTemplate, redisTemplate, objectMapper, "prices");

        // Payload without eventTime "E"
        String payload = "{\"s\":\"BTCUSDT\",\"c\":\"16800.50\"}";
        publisher.processAndPublish(payload);

        verify(kafkaTemplate).send(eq("prices"), eq("BTCUSDT"), argThat(event -> {
            PriceEvent pe = (PriceEvent) event;
            return pe.getTimestamp() > 0 && pe.getPrice().equals(new BigDecimal("16800.50"));
        }));
    }
}
