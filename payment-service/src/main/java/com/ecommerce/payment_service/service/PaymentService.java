package com.ecommerce.payment_service.service;

import com.ecommerce.payment_service.dto.PaymentRequestDto;
import com.ecommerce.payment_service.dto.PaymentResponseDto;

public interface PaymentService {
    PaymentResponseDto initiatePayment(PaymentRequestDto dto);
    PaymentResponseDto getPaymentById(Long id);
    PaymentResponseDto refundPayment(Long id);
}
