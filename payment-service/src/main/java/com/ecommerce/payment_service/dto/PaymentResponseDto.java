package com.ecommerce.payment_service.dto;

import com.ecommerce.payment_service.model.PaymentStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class PaymentResponseDto {
    private Long id;
    private Long orderId;
    private Long userId;
    private Double amount;
    private String currency;
    private String provider;
    private String providerPaymentId;
    private PaymentStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
