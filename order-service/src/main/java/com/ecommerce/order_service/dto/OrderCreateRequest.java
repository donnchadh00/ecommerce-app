package com.ecommerce.order_service.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record OrderCreateRequest(
        Long userId,

        @NotEmpty(message = "items are required")
        List<@Valid OrderItemRequest> items
) {
    public record OrderItemRequest(
            @NotNull(message = "productId is required")
            Long productId,

            @NotNull(message = "quantity is required")
            @Positive(message = "quantity must be greater than zero")
            Integer quantity
    ) {
    }
}
