package com.ecommerce.order_service.service;

import com.ecommerce.order_service.model.Order;
import com.ecommerce.order_service.model.OrderItem;
import com.ecommerce.order_service.repository.OrderRepository;
import com.ecommerce.common.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;


@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final JwtService jwtService;

    public Order createOrder(Order order, HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        Long userId = jwtService.extractUserId(token);
        order.setUserId(userId);

        for (OrderItem item : order.getItems()) {
            item.setOrder(order);
        }

        return orderRepository.save(order);
    }

    public List<Order> getOrdersForUser(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        Long userId = jwtService.extractUserId(token);

        return orderRepository.findByUserId(userId);
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }
}
