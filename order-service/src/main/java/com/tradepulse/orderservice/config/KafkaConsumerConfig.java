package com.tradepulse.orderservice.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.ExponentialBackOff;

@Configuration
public class KafkaConsumerConfig {

    @Bean
    public NewTopic ordersTopic() {
        return TopicBuilder.name("orders")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic pricesTopic() {
        return TopicBuilder.name("prices")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public CommonErrorHandler errorHandler(KafkaTemplate<Object, Object> kafkaTemplate) {
        // Publishes poison/failed messages to <originalTopic>.DLT
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate);
        
        // 3 retries (4 total attempts) with exponential backoff: 1000ms base, 2.0 multiplier
        ExponentialBackOff backOff = new ExponentialBackOff(1000L, 2.0);
        backOff.setMaxAttempts(4);
        
        return new DefaultErrorHandler(recoverer, backOff);
    }
}
