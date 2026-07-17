package com.tradepulse.priceingestion.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradepulse.priceingestion.event.PriceEvent;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
public class PricePublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final String pricesTopic;

    @Autowired
    public PricePublisher(KafkaTemplate<String, Object> kafkaTemplate,
                          @Autowired(required = false) StringRedisTemplate redisTemplate,
                          ObjectMapper objectMapper,
                          @Value("${app.kafka.topics.prices:prices}") String pricesTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.pricesTopic = pricesTopic;
    }

    public void processAndPublish(String payload) {
        try {
            BinanceTicker ticker = objectMapper.readValue(payload, BinanceTicker.class);
            if (ticker.getSymbol() == null || ticker.getLastPrice() == null) {
                log.warn("Invalid ticker payload received: {}", payload);
                return;
            }

            String symbol = ticker.getSymbol().toUpperCase();
            BigDecimal price = new BigDecimal(ticker.getLastPrice());
            long timestamp = ticker.getEventTime() != null ? ticker.getEventTime() : System.currentTimeMillis();

            // 1. Cache to Redis
            if (redisTemplate != null) {
                try {
                    redisTemplate.opsForValue().set("price:" + symbol, price.toString());
                } catch (Exception e) {
                    log.warn("Redis is unavailable, skipping cache for {}: {}", symbol, e.getMessage());
                }
            }

            // 2. Publish to Kafka
            PriceEvent event = PriceEvent.builder()
                    .symbol(symbol)
                    .price(price)
                    .timestamp(timestamp)
                    .build();

            log.info("Ingested price tick: {} @ {}", symbol, price);
            kafkaTemplate.send(pricesTopic, symbol, event);

        } catch (Exception e) {
            log.error("Failed to parse or publish ticker payload: {}", payload, e);
        }
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BinanceTicker {
        @JsonProperty("s")
        private String symbol;
        @JsonProperty("c")
        private String lastPrice;
        @JsonProperty("E")
        private Long eventTime;
    }
}
