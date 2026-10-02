package com.muzaffar.orders.notification;

import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * Keeps the latest notifications in memory so they can be inspected over REST.
 * In a real system this would send email/SMS/push and store a delivery log.
 */
@Component
public class NotificationStore {

    static final int CAPACITY = 200;

    private final Deque<Notification> latest = new ArrayDeque<>();

    public synchronized void add(Notification notification) {
        latest.addFirst(notification);
        while (latest.size() > CAPACITY) {
            latest.removeLast();
        }
    }

    public synchronized List<Notification> latest(String customerId) {
        return latest.stream()
                .filter(n -> customerId == null || customerId.equals(n.customerId()))
                .toList();
    }
}
