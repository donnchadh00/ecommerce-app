package com.ecommerce.payment_service.mapper;

import com.ecommerce.payment_service.dto.PaymentRequestDto;
import com.ecommerce.payment_service.dto.PaymentResponseDto;
import com.ecommerce.payment_service.model.Payment;
import com.ecommerce.payment_service.model.PaymentStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public class PaymentMapper {

    public static Payment toEntity(PaymentRequestDto dto) {
        return Payment.builder()
            .orderId(dto.getOrderId())
            .userId(dto.getUserId())
            .amount(dto.getAmount())
            .currency(dto.getCurrency())
            .provider("mock")
            .providerPaymentId(UUID.randomUUID().toString())
            .status(PaymentStatus.SUCCESSFUL)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
    }

    public static PaymentResponseDto toDto(Payment payment) {
        return PaymentResponseDto.builder()
            .id(payment.getId())
            .orderId(payment.getOrderId())
            .userId(payment.getUserId())
            .amount(payment.getAmount())
            .currency(payment.getCurrency())
            .provider(payment.getProvider())
            .providerPaymentId(payment.getProviderPaymentId())
            .status(payment.getStatus())
            .createdAt(payment.getCreatedAt())
            .updatedAt(payment.getUpdatedAt())
            .build();
    }
}
