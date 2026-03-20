package com.ecommerce.order_service.controller;

import com.ecommerce.order_service.dto.OrderAcceptedResponse;
import com.ecommerce.order_service.dto.OrderCreateRequest;
import com.ecommerce.order_service.dto.OrderResponse;
import com.ecommerce.order_service.dto.TraceTimelineResponse;
import com.ecommerce.order_service.service.CurrentTraceContext;
import com.ecommerce.order_service.service.OrderService;
import com.ecommerce.order_service.service.TempoTraceService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private static final String TRACE_HEADER = "X-Trace-Id";
    private static final Pattern TRACE_ID_PATTERN = Pattern.compile("^[0-9a-f]{32}$");

    private final CurrentTraceContext currentTraceContext;
    private final OrderService orderService;
    private final TempoTraceService tempoTraceService;

    @PostMapping
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<OrderAcceptedResponse> placeOrder(@Valid @RequestBody OrderCreateRequest order, HttpServletRequest request) {
        // Prefer the real active trace id so the confirmation page can query Tempo.
        String traceId = currentTraceContext.currentTraceId()
                                 .or(() -> Optional.ofNullable(request.getHeader(TRACE_HEADER))
                                     .filter(OrderController::isTempoTraceId))
                                 .orElseGet(() -> UUID.randomUUID().toString().replace("-", ""));

        OrderResponse created = orderService.createOrder(order, request, traceId);

        // Return 202 since fulfillment continues asynchronously via events
        return ResponseEntity.accepted()
                .location(URI.create("/api/orders/" + created.id()))
                .headers(h -> h.add(TRACE_HEADER, traceId))
                .body(new OrderAcceptedResponse(created.id(), created.status(), traceId));
    }

    private static boolean isTempoTraceId(String candidate) {
        return candidate != null && TRACE_ID_PATTERN.matcher(candidate).matches();
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

    @GetMapping("/traces/{traceId}")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<TraceTimelineResponse> getTraceTimeline(@PathVariable String traceId) {
        return ResponseEntity.ok(tempoTraceService.fetchTimeline(traceId));
    }
}
