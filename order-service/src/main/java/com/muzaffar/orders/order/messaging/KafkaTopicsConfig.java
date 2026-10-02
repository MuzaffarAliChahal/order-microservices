package com.muzaffar.orders.order.messaging;

import com.muzaffar.orders.events.Topics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicsConfig {

    @Bean
    NewTopic ordersTopic() {
        return TopicBuilder.name(Topics.ORDERS).partitions(3).replicas(1).build();
    }

    @Bean
    NewTopic paymentsTopic() {
        return TopicBuilder.name(Topics.PAYMENTS).partitions(3).replicas(1).build();
    }
}
