package com.tradepulse.orderservice;

import com.tradepulse.orderservice.model.Order;
import com.tradepulse.orderservice.model.OrderSide;
import com.tradepulse.orderservice.model.OrderStatus;
import com.tradepulse.orderservice.model.OrderType;
import com.tradepulse.orderservice.service.OrderEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class OrderEventPublisherTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private OrderEventPublisher publisher;
    private OrderEventPublisher publisherNoKafka;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        publisher = new OrderEventPublisher(kafkaTemplate);
        publisherNoKafka = new OrderEventPublisher(null);
    }

    private Order buildOrder() {
        return Order.builder()
                .id("order-123")
                .symbol("BTCUSDT")
                .side(OrderSide.BUY)
                .type(OrderType.MARKET)
                .quantity(BigDecimal.ONE)
                .price(BigDecimal.valueOf(50000))
                .status(OrderStatus.PENDING)
                .build();
    }

    @Test
    void testPublishOrderEvent_withKafka_sendsMessage() {
        Order order = buildOrder();

        publisher.publishOrderEvent("ORDER_PLACED", order);

        // ordersTopic is @Value-injected and is null in a plain unit test (no Spring context)
        verify(kafkaTemplate).send(isNull(), eq("BTCUSDT"), any());
    }

    @Test
    void testPublishOrderEvent_withoutKafka_doesNotThrow() {
        Order order = buildOrder();

        assertDoesNotThrow(() -> publisherNoKafka.publishOrderEvent("ORDER_PLACED", order));
        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void testPublishOrderEvent_kafkaThrows_doesNotPropagate() {
        Order order = buildOrder();
        doThrow(new RuntimeException("Kafka unavailable")).when(kafkaTemplate).send(any(String.class), any(), any());

        assertDoesNotThrow(() -> publisher.publishOrderEvent("ORDER_PLACED", order));
    }
}
