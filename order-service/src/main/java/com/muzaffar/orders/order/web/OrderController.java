package com.muzaffar.orders.order.web;

import com.muzaffar.orders.order.domain.Order;
import com.muzaffar.orders.order.service.OrderNotFoundException;
import com.muzaffar.orders.order.service.OrderService;
import com.muzaffar.orders.order.web.OrderDtos.OrderResponse;
import com.muzaffar.orders.order.web.OrderDtos.PlaceOrderRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService service;

    public OrderController(OrderService service) {
        this.service = service;
    }

    /** Returns 202 Accepted: the order is PENDING until payment-service answers. */
    @PostMapping
    public ResponseEntity<OrderResponse> place(@Valid @RequestBody PlaceOrderRequest request) {
        Order order = service.place(request.customerId(), request.product(), request.quantity(), request.unitPrice());
        return ResponseEntity.accepted()
                .location(URI.create("/api/orders/" + order.getId()))
                .body(OrderResponse.from(order));
    }

    @GetMapping("/{id}")
    public OrderResponse get(@PathVariable Long id) {
        return OrderResponse.from(service.get(id));
    }

    @GetMapping
    public List<OrderResponse> list(@RequestParam(required = false) String customerId) {
        return service.recent(customerId).stream().map(OrderResponse::from).toList();
    }

    @ExceptionHandler(OrderNotFoundException.class)
    public ProblemDetail notFound(OrderNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }
}
