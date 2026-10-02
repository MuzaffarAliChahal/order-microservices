package com.muzaffar.orders.events;

import java.math.BigDecimal;
import java.time.Instant;

/** Published by payment-service to {@link Topics#PAYMENTS} with the result of charging an order. */
public record PaymentEvent(Long orderId, String customerId, BigDecimal amount, boolean success,
                           String reason, Instant processedAt) {
}
