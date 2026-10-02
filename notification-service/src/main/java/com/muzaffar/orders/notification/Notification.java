package com.muzaffar.orders.notification;

import java.time.Instant;

public record Notification(String type, Long orderId, String customerId, String message, Instant sentAt) {
}
