package com.muzaffar.orders.order.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.muzaffar.orders.events.PaymentEvent;
import com.muzaffar.orders.events.Topics;
import com.muzaffar.orders.order.service.OrderService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** Completes the saga: confirms or cancels the order when payment-service reports back. */
@Component
public class PaymentEventListener {

    private final OrderService orderService;
    private final ObjectMapper json;

    public PaymentEventListener(OrderService orderService, ObjectMapper json) {
        this.orderService = orderService;
        this.json = json;
    }

    @KafkaListener(topics = Topics.PAYMENTS, groupId = "order-service")
    public void onPayment(String message) throws JsonProcessingException {
        orderService.applyPayment(json.readValue(message, PaymentEvent.class));
    }
}
