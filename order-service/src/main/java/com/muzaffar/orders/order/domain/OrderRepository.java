package com.muzaffar.orders.order.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findTop50ByCustomerIdOrderByCreatedAtDesc(String customerId);

    List<Order> findTop50ByOrderByCreatedAtDesc();
}
