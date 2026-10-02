package com.muzaffar.orders.order.service;

import com.muzaffar.orders.events.OrderCreatedEvent;
import com.muzaffar.orders.events.PaymentEvent;
import com.muzaffar.orders.order.domain.Order;
import com.muzaffar.orders.order.domain.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orders;
    private final ApplicationEventPublisher events;

    public OrderService(OrderRepository orders, ApplicationEventPublisher events) {
        this.orders = orders;
        this.events = events;
    }

    @Transactional
    public Order place(String customerId, String product, int quantity, BigDecimal unitPrice) {
        Order order = orders.save(new Order(customerId, product, quantity, unitPrice));
        events.publishEvent(new OrderPlaced(new OrderCreatedEvent(order.getId(), order.getCustomerId(),
                order.getProduct(), order.getQuantity(), order.getTotal(), order.getCreatedAt())));
        log.info("Order {} placed for customer {} (total {})", order.getId(), customerId, order.getTotal());
        return order;
    }

    @Transactional
    public void applyPayment(PaymentEvent payment) {
        orders.findById(payment.orderId()).ifPresentOrElse(order -> {
            if (order.applyPayment(payment.success(), payment.reason())) {
                log.info("Order {} is now {}", order.getId(), order.getStatus());
            } else {
                log.info("Order {} already {}, ignoring duplicate payment event", order.getId(), order.getStatus());
            }
        }, () -> log.warn("Payment received for unknown order {}", payment.orderId()));
    }

    @Transactional(readOnly = true)
    public Order get(Long id) {
        return orders.findById(id).orElseThrow(() -> new OrderNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<Order> recent(String customerId) {
        return customerId == null
                ? orders.findTop50ByOrderByCreatedAtDesc()
                : orders.findTop50ByCustomerIdOrderByCreatedAtDesc(customerId);
    }
}
