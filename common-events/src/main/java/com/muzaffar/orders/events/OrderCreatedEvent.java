package com.muzaffar.orders.events;

import java.math.BigDecimal;
import java.time.Instant;

/** Published by order-service to {@link Topics#ORDERS} after an order is saved. */
public record OrderCreatedEvent(Long orderId, String customerId, String product, int quantity,
                                BigDecimal total, Instant createdAt) {
}
