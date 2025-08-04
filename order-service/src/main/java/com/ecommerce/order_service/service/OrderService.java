package com.ecommerce.order_service.service;

import com.ecommerce.order_service.model.Order;
import com.ecommerce.order_service.model.OrderItem;
import com.ecommerce.order_service.repository.OrderRepository;
import com.ecommerce.order_service.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import jakarta.servlet.http.HttpServletRequest;

import java.security.Principal;
import java.util.List;
import java.util.ArrayList;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.http.HttpServletRequest;

import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;

    private static final String SECRET_KEY = "mysecretkeymysecretkeymysecretkeymysecretkey";

    public Long getUserIdFromJwt(HttpServletRequest request) {
        String token = request.getHeader("Authorization").replace("Bearer ", "");

        Claims claims = Jwts.parser()
            .setSigningKey(SECRET_KEY.getBytes(StandardCharsets.UTF_8))
            .parseClaimsJws(token)
            .getBody();

        return claims.get("userId", Long.class);
    }

    public Order createOrder(Order order, HttpServletRequest request) {
        Long userId = getUserIdFromJwt(request);
        order.setUserId(userId);

        for (OrderItem item : order.getItems()) {
            item.setOrder(order);
        }

        return orderRepository.save(order);
    }

    public List<Order> getOrdersForUser(HttpServletRequest request) {
        Long userId = getUserIdFromJwt(request);

        return orderRepository.findByUserId(userId);
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }
}
