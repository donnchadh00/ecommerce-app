package com.ecommerce.order_service.controller;

import com.ecommerce.order_service.dto.OrderAcceptedResponse;
import com.ecommerce.order_service.dto.OrderCreateRequest;
import com.ecommerce.order_service.dto.OrderResponse;
import com.ecommerce.order_service.service.OrderService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private static final String TRACE_HEADER = "X-Trace-Id";

    private final OrderService orderService;

    @PostMapping
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<OrderAcceptedResponse> placeOrder(@Valid @RequestBody OrderCreateRequest order, HttpServletRequest request) {
        // propagate or create a trace id for cross-service correlation
        String traceId = Optional.ofNullable(request.getHeader(TRACE_HEADER))
                                 .filter(h -> !h.isBlank())
                                 .orElse(UUID.randomUUID().toString());

        OrderResponse created = orderService.createOrder(order, request, traceId);

        // Return 202 since fulfillment continues asynchronously via events
        return ResponseEntity.accepted()
                .location(URI.create("/api/orders/" + created.id()))
                .headers(h -> h.add(TRACE_HEADER, traceId))
                .body(new OrderAcceptedResponse(created.id(), created.status(), traceId));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<List<OrderResponse>> getUserOrders(HttpServletRequest request) {
        List<OrderResponse> orders = orderService.getOrdersForUser(request);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<OrderResponse>> getAllOrders() {
        List<OrderResponse> orders = orderService.getAllOrders();
        return ResponseEntity.ok(orders);
    }
}
