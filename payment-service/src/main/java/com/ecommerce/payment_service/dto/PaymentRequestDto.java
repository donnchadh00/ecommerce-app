package com.ecommerce.payment_service.dto;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class PaymentRequestDto {
    private Long orderId;
    private Long userId;
    private BigDecimal amount;
    private String currency;
}
