package com.muzaffar.orders.gateway;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }

    /** Single entry point: clients call the gateway, the gateway forwards to the right service. */
    @Bean
    RouteLocator routes(RouteLocatorBuilder builder,
                        @Value("${routes.order-service}") String orderService,
                        @Value("${routes.notification-service}") String notificationService) {
        return builder.routes()
                .route("order-service", r -> r.path("/api/orders/**", "/api/orders").uri(orderService))
                .route("notification-service", r -> r.path("/api/notifications/**", "/api/notifications")
                        .uri(notificationService))
                .build();
    }
}
