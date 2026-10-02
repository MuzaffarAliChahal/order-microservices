package com.muzaffar.orders.order.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String customerId;

    @Column(nullable = false)
    private String product;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private OrderStatus status;

    private String failureReason;

    @Version
    private long version;

    private Instant createdAt;
    private Instant updatedAt;

    protected Order() {
    }

    public Order(String customerId, String product, int quantity, BigDecimal unitPrice) {
        this.customerId = customerId;
        this.product = product;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.total = unitPrice.multiply(BigDecimal.valueOf(quantity));
        this.status = OrderStatus.PENDING;
    }

    @PrePersist
    void onCreate() {
        createdAt = updatedAt = Instant.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    /**
     * Applies a payment result. Only PENDING orders change, so a redelivered Kafka message is harmless.
     *
     * @return true if the status changed
     */
    public boolean applyPayment(boolean success, String reason) {
        if (status != OrderStatus.PENDING) {
            return false;
        }
        if (success) {
            status = OrderStatus.CONFIRMED;
        } else {
            status = OrderStatus.CANCELLED;
            failureReason = reason;
        }
        return true;
    }

    public Long getId() { return id; }
    public String getCustomerId() { return customerId; }
    public String getProduct() { return product; }
    public int getQuantity() { return quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public BigDecimal getTotal() { return total; }
    public OrderStatus getStatus() { return status; }
    public String getFailureReason() { return failureReason; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
