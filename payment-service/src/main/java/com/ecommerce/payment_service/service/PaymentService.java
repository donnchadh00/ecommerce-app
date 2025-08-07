package com.ecommerce.payment_service.service;

import com.ecommerce.payment_service.model.Payment;

public interface PaymentService {
    Payment initiatePayment(Payment payment);
    Payment getPaymentById(Long id);
    Payment refundPayment(Long id);
}
