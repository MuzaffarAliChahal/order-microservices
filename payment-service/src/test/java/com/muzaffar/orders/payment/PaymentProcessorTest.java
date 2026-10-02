package com.muzaffar.orders.payment;

import com.muzaffar.orders.events.OrderCreatedEvent;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentProcessorTest {

    private final PaymentProcessor processor = new PaymentProcessor(new BigDecimal("1000.00"));

    private static OrderCreatedEvent order(long id, String total) {
        return new OrderCreatedEvent(id, "c-1", "Item", 1, new BigDecimal(total), Instant.now());
    }

    @Test
    void approvesAmountsWithinLimit() {
        var result = processor.process(order(1, "1000.00")).orElseThrow();
        assertThat(result.success()).isTrue();
        assertThat(result.reason()).isNull();
    }

    @Test
    void declinesAmountsAboveLimit() {
        var result = processor.process(order(2, "1000.01")).orElseThrow();
        assertThat(result.success()).isFalse();
        assertThat(result.reason()).contains("exceeds limit");
    }

    @Test
    void ignoresDuplicateOrders() {
        assertThat(processor.process(order(3, "10"))).isPresent();
        assertThat(processor.process(order(3, "10"))).isEmpty();
    }
}
