package com.muzaffar.orders.notification;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationStoreTest {

    private static Notification n(long orderId, String customer) {
        return new Notification("ORDER_RECEIVED", orderId, customer, "msg", Instant.now());
    }

    @Test
    void returnsNewestFirstAndFiltersByCustomer() {
        NotificationStore store = new NotificationStore();
        store.add(n(1, "a"));
        store.add(n(2, "b"));
        store.add(n(3, "a"));

        assertThat(store.latest(null)).extracting(Notification::orderId).containsExactly(3L, 2L, 1L);
        assertThat(store.latest("a")).extracting(Notification::orderId).containsExactly(3L, 1L);
    }

    @Test
    void keepsOnlyTheLatestEntries() {
        NotificationStore store = new NotificationStore();
        for (long i = 0; i < NotificationStore.CAPACITY + 50; i++) {
            store.add(n(i, "a"));
        }
        assertThat(store.latest(null)).hasSize(NotificationStore.CAPACITY);
        assertThat(store.latest(null).get(0).orderId()).isEqualTo(NotificationStore.CAPACITY + 49L);
    }
}
