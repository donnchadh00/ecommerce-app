package com.ecommerce.order_service.controller;

import com.ecommerce.order_service.model.Order;
import com.ecommerce.order_service.service.OrderService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import java.util.Optional;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private static final String TRACE_HEADER = "X-Trace-Id";

    private final OrderService orderService;

    @PostMapping
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<Map<String, Object>> placeOrder(@RequestBody Order order, HttpServletRequest request) {
        // propagate or create a trace id for cross-service correlation
        String traceId = Optional.ofNullable(request.getHeader(TRACE_HEADER))
                                 .filter(h -> !h.isBlank())
                                 .orElse(UUID.randomUUID().toString());

        Order created = orderService.createOrder(order, request, traceId);

        // Return 202 since fulfillment continues asynchronously via events
        return ResponseEntity.accepted()
                .location(URI.create("/api/orders/" + created.getId()))
                .headers(h -> h.add(TRACE_HEADER, traceId))
                .body(Map.of(
                        "orderId", created.getId(),
                        "status", created.getStatus(),
                        "traceId", traceId
                ));
    }

    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<Order>> getUserOrders(HttpServletRequest request) {
        List<Order> orders = orderService.getOrdersForUser(request);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Order>> getAllOrders() {
        List<Order> orders = orderService.getAllOrders();
        return ResponseEntity.ok(orders);
    }
}
