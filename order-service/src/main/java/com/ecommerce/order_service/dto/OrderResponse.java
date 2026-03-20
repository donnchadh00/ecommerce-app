package com.ecommerce.order_service.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Long id,
        Long userId,
        LocalDateTime createdAt,
        String status,
        String traceId,
        BigDecimal total,
        List<OrderItemResponse> items
) {
    public record OrderItemResponse(
            Long id,
            Long productId,
            Integer quantity,
            BigDecimal unitPrice
    ) {
    }
}
