package com.ecommerce.payment_service.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

public record PaymentRequestDto(
        @NotNull(message = "orderId is required")
        Long orderId,

        @NotNull(message = "userId is required")
        Long userId,

        @NotNull(message = "amount is required")
        @DecimalMin(value = "0.01", inclusive = true, message = "amount must be at least 0.01")
        @Digits(integer = 10, fraction = 2, message = "amount must have up to 2 decimal places")
        BigDecimal amount,

        @NotBlank(message = "currency is required")
        @Pattern(regexp = "^[A-Za-z]{3}$", message = "currency must be a 3-letter code")
        String currency
) {
}
