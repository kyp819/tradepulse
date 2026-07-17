package com.tradepulse.orderservice.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradepulse.orderservice.service.OrderExecutionEngine;
import com.tradepulse.orderservice.service.PriceCacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Slf4j
public class PriceEventListener {

    private final PriceCacheService priceCacheService;
    private final OrderExecutionEngine executionEngine;
    private final ObjectMapper objectMapper;

    @Autowired
    public PriceEventListener(PriceCacheService priceCacheService,
                              OrderExecutionEngine executionEngine,
                              ObjectMapper objectMapper) {
        this.priceCacheService = priceCacheService;
        this.executionEngine = executionEngine;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "${app.kafka.topics.prices:prices}", groupId = "${spring.kafka.consumer.group-id:order-service-group}")
    public void consumePrice(String message) throws IOException {
        PriceEvent priceEvent = objectMapper.readValue(message, PriceEvent.class);
        log.debug("Received price tick: {} @ {}", priceEvent.getSymbol(), priceEvent.getPrice());

        // 1. Update latest price cache
        priceCacheService.updatePrice(priceEvent.getSymbol(), priceEvent.getPrice());

        // 2. Run matching logic for pending orders
        executionEngine.matchPendingOrders(priceEvent.getSymbol(), priceEvent.getPrice());
    }
}

