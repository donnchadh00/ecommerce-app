package com.ecommerce.payment_service.dto;

import lombok.Data;

@Data
public class PaymentRequestDto {
    private Long orderId;
    private Long userId;
    private Double amount;
    private String currency;
}
