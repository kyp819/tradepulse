package com.tradepulse.orderservice.service;

import com.tradepulse.orderservice.model.Order;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class OrderEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.topics.orders:orders}")
    private String ordersTopic;

    @Autowired(required = false)
    public OrderEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishOrderEvent(String eventType, Order order) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", eventType);
        event.put("orderId", order.getId());
        event.put("symbol", order.getSymbol());
        event.put("side", order.getSide().name());
        event.put("type", order.getType().name());
        event.put("price", order.getPrice());
        event.put("quantity", order.getQuantity());
        event.put("status", order.getStatus().name());
        event.put("timestamp", System.currentTimeMillis());

        log.info("Publishing Kafka event {} for order {}", eventType, order.getId());

        if (kafkaTemplate != null) {
            try {
                kafkaTemplate.send(ordersTopic, order.getSymbol().toUpperCase(), event);
            } catch (Exception e) {
                log.error("Failed to send Kafka event to topic {}: {}", ordersTopic, e.getMessage());
            }
        } else {
            log.info("KafkaTemplate not initialized (running without Kafka). Event content: {}", event);
        }
    }
}
