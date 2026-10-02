package com.muzaffar.orders.order.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.muzaffar.orders.events.Topics;
import com.muzaffar.orders.order.service.OrderPlaced;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Sends OrderCreatedEvent to Kafka only after the database transaction commits,
 * so payment-service never receives an event for an order that was rolled back.
 */
@Component
public class OrderEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(OrderEventPublisher.class);

    private final KafkaTemplate<String, String> kafka;
    private final ObjectMapper json;

    public OrderEventPublisher(KafkaTemplate<String, String> kafka, ObjectMapper json) {
        this.kafka = kafka;
        this.json = json;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderPlaced(OrderPlaced placed) throws JsonProcessingException {
        var event = placed.event();
        kafka.send(Topics.ORDERS, String.valueOf(event.orderId()), json.writeValueAsString(event))
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish order {}", event.orderId(), ex);
                    }
                });
    }
}
