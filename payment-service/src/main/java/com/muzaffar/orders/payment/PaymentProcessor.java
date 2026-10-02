package com.muzaffar.orders.payment;

import com.muzaffar.orders.events.OrderCreatedEvent;
import com.muzaffar.orders.events.PaymentEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simulated payment gateway: charges succeed up to a per-order limit.
 * Keeps track of processed order ids so a redelivered Kafka message is not charged twice.
 */
@Component
public class PaymentProcessor {

    private final BigDecimal limit;
    private final Set<Long> processed = ConcurrentHashMap.newKeySet();

    public PaymentProcessor(@Value("${payment.limit:1000.00}") BigDecimal limit) {
        this.limit = limit;
    }

    /** @return the payment result, or empty if this order was already processed */
    public Optional<PaymentEvent> process(OrderCreatedEvent order) {
        if (!processed.add(order.orderId())) {
            return Optional.empty();
        }
        boolean success = order.total().compareTo(limit) <= 0;
        String reason = success ? null : "amount " + order.total().toPlainString()
                + " exceeds limit of " + limit.toPlainString();
        return Optional.of(new PaymentEvent(order.orderId(), order.customerId(), order.total(), success, reason,
                Instant.now()));
    }
}
