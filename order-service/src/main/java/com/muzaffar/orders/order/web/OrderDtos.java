package com.muzaffar.orders.order.web;

import com.muzaffar.orders.order.domain.Order;
import com.muzaffar.orders.order.domain.OrderStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;

public final class OrderDtos {

    private OrderDtos() {
    }

    public record PlaceOrderRequest(
            @NotBlank @Size(max = 64) String customerId,
            @NotBlank @Size(max = 255) String product,
            @NotNull @Min(1) @Max(1000) Integer quantity,
            @NotNull @DecimalMin("0.01") BigDecimal unitPrice) {
    }

    public record OrderResponse(Long id, String customerId, String product, int quantity, BigDecimal unitPrice,
                                BigDecimal total, OrderStatus status, String failureReason,
                                Instant createdAt, Instant updatedAt) {

        public static OrderResponse from(Order o) {
            return new OrderResponse(o.getId(), o.getCustomerId(), o.getProduct(), o.getQuantity(),
                    o.getUnitPrice(), o.getTotal(), o.getStatus(), o.getFailureReason(),
                    o.getCreatedAt(), o.getUpdatedAt());
        }
    }
}
