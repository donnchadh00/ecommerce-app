package com.ecommerce.product_service.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ProductCreateRequest(
        @NotBlank(message = "name is required")
        @Size(max = 120, message = "name must be 120 characters or fewer")
        String name,

        @Size(max = 1000, message = "description must be 1000 characters or fewer")
        String description,

        @NotNull(message = "price is required")
        @DecimalMin(value = "0.00", inclusive = false, message = "price must be greater than zero")
        @Digits(integer = 10, fraction = 2, message = "price must have up to 2 decimal places")
        BigDecimal price,

        @NotNull(message = "quantity is required")
        @Min(value = 0, message = "quantity must be zero or greater")
        Integer quantity
) {
}
