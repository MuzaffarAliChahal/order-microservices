package com.muzaffar.orders.notification;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.muzaffar.orders.events.OrderCreatedEvent;
import com.muzaffar.orders.events.PaymentEvent;
import com.muzaffar.orders.events.Topics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class NotificationListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationListener.class);

    private final NotificationStore store;
    private final ObjectMapper json;

    public NotificationListener(NotificationStore store, ObjectMapper json) {
        this.store = store;
        this.json = json;
    }

    @KafkaListener(topics = Topics.ORDERS, groupId = "notification-service")
    public void onOrderCreated(String message) throws JsonProcessingException {
        OrderCreatedEvent e = json.readValue(message, OrderCreatedEvent.class);
        send(new Notification("ORDER_RECEIVED", e.orderId(), e.customerId(),
                "We received your order #" + e.orderId() + " for " + e.quantity() + " x " + e.product()
                        + " (total " + e.total().toPlainString() + ").", Instant.now()));
    }

    @KafkaListener(topics = Topics.PAYMENTS, groupId = "notification-service")
    public void onPayment(String message) throws JsonProcessingException {
        PaymentEvent e = json.readValue(message, PaymentEvent.class);
        String text = e.success()
                ? "Payment received. Order #" + e.orderId() + " is confirmed."
                : "Payment failed for order #" + e.orderId() + ": " + e.reason() + ". The order was cancelled.";
        send(new Notification(e.success() ? "ORDER_CONFIRMED" : "ORDER_CANCELLED", e.orderId(), e.customerId(),
                text, Instant.now()));
    }

    private void send(Notification n) {
        store.add(n);
        log.info("[{}] to {}: {}", n.type(), n.customerId(), n.message());
    }
}
