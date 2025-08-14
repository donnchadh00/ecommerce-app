package com.ecommerce.order_service.service;

import com.ecommerce.order_service.model.Order;
import com.ecommerce.order_service.model.OrderItem;
import com.ecommerce.order_service.repository.OrderRepository;
import com.ecommerce.order_service.outbox.EventOutbox;

import com.ecommerce.common.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpHeaders;

import com.ecommerce.events.order.OrderPlaced;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final JwtService jwtService;
    private final EventOutbox eventOutbox;

    @Transactional
    public Order createOrder(Order order, HttpServletRequest request, String traceId) {
        // 1) Resolve userId (prefer JWT; fallback to payload if present)
        Long userId = null;
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                userId = jwtService.extractUserId(authHeader.substring(7));
            } catch (Exception ignored) {}
        }
        if (userId == null) {
            userId = order.getUserId(); // fallback during local testing
        }
        if (userId == null) {
            throw new IllegalArgumentException("userId is required (JWT or request body)");
        }
        order.setUserId(userId);

        // 2) Ensure status defaults to PENDING (only if blank)
        trySetPending(order);

        // 3) Normalize items list & set back‑refs (FK), validate quantities
        if (order.getItems() == null) {
            order.setItems(new ArrayList<>());
        }
        for (OrderItem item : order.getItems()) {
            if (item == null) continue;
            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new IllegalArgumentException("Each item must have quantity > 0");
            }
            item.setId(null);       // force INSERT even if client sent an id
            item.setOrder(order);      // establish FK (order_id)
        }

        // 4) Persist order + items (cascade)
        Order saved = orderRepository.save(order);

        // 5) Build event payload (lines + total)
        var lines = saved.getItems().stream()
            .map(i -> new OrderPlaced.Line(String.valueOf(i.getProductId()), i.getQuantity()))
            .toList();

        BigDecimal total = computeTotal(saved);

        OrderPlaced evt = new OrderPlaced(
            String.valueOf(saved.getId()),
            String.valueOf(userId),
            lines,
            total
        );

        // 6) Write to outbox inside the SAME TX
        eventOutbox.save(
            String.valueOf(saved.getId()),
            "order.v1.placed",
            evt,
            traceId
        );

        return saved;
    }


    @Transactional(readOnly = true)
    public List<Order> getOrdersForUser(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        Long userId = jwtService.extractUserId(token);

        return orderRepository.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    // helpers

    private void trySetPending(Order order) {
        try {
            var statusField = order.getClass().getMethod("setStatus", String.class);
            statusField.invoke(order, "PENDING");
        } catch (Exception ignored) {}
    }

    // Temp: Set price of all items to 10usd
    private BigDecimal computeTotal(Order order) {
        BigDecimal total = order.getItems().stream()
            .map(i -> BigDecimal.valueOf(10).multiply(BigDecimal.valueOf(i.getQuantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        return total;
    }
}
