package com.muzaffar.orders.order;

import com.muzaffar.orders.order.domain.Order;
import com.muzaffar.orders.order.domain.OrderStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class OrderTest {

    @Test
    void calculatesTotalAndStartsPending() {
        Order order = new Order("c-1", "Keyboard", 3, new BigDecimal("49.99"));
        assertThat(order.getTotal()).isEqualByComparingTo("149.97");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    void successfulPaymentConfirms() {
        Order order = new Order("c-1", "Keyboard", 1, BigDecimal.TEN);
        assertThat(order.applyPayment(true, null)).isTrue();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    void failedPaymentCancelsWithReason() {
        Order order = new Order("c-1", "Keyboard", 1, BigDecimal.TEN);
        order.applyPayment(false, "limit exceeded");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.getFailureReason()).isEqualTo("limit exceeded");
    }

    @Test
    void duplicatePaymentEventsAreIgnored() {
        Order order = new Order("c-1", "Keyboard", 1, BigDecimal.TEN);
        order.applyPayment(true, null);
        assertThat(order.applyPayment(false, "late duplicate")).isFalse();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }
}
