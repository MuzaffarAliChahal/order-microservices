package com.muzaffar.orders.payment;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.muzaffar.orders.events.OrderCreatedEvent;
import com.muzaffar.orders.events.Topics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class OrderEventListener {

    private static final Logger log = LoggerFactory.getLogger(OrderEventListener.class);

    private final PaymentProcessor processor;
    private final KafkaTemplate<String, String> kafka;
    private final ObjectMapper json;

    public OrderEventListener(PaymentProcessor processor, KafkaTemplate<String, String> kafka, ObjectMapper json) {
        this.processor = processor;
        this.kafka = kafka;
        this.json = json;
    }

    @KafkaListener(topics = Topics.ORDERS, groupId = "payment-service")
    public void onOrderCreated(String message) throws JsonProcessingException {
        OrderCreatedEvent order = json.readValue(message, OrderCreatedEvent.class);
        processor.process(order).ifPresentOrElse(payment -> {
            try {
                kafka.send(Topics.PAYMENTS, String.valueOf(payment.orderId()), json.writeValueAsString(payment));
                log.info("Order {} payment {}", payment.orderId(), payment.success() ? "succeeded" : "declined");
            } catch (JsonProcessingException e) {
                throw new IllegalStateException(e);
            }
        }, () -> log.info("Order {} already processed, skipping", order.orderId()));
    }
}
