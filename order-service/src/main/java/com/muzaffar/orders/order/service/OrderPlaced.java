package com.muzaffar.orders.order.service;

import com.muzaffar.orders.events.OrderCreatedEvent;

/** Spring application event, published inside the transaction and forwarded to Kafka after commit. */
public record OrderPlaced(OrderCreatedEvent event) {
}
