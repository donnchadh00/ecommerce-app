package com.ecommerce.payment_service.service.impl;

import com.ecommerce.payment_service.dto.PaymentRequestDto;
import com.ecommerce.payment_service.dto.PaymentResponseDto;
import com.ecommerce.payment_service.mapper.PaymentMapper;
import com.ecommerce.payment_service.model.Payment;
import com.ecommerce.payment_service.model.PaymentStatus;
import com.ecommerce.payment_service.repository.PaymentRepository;
import com.ecommerce.payment_service.service.PaymentService;
import com.ecommerce.payment_service.service.StripePaymentService;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.param.RefundCreateParams;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final StripePaymentService stripePaymentService;

    @Override
    public PaymentResponseDto initiatePayment(PaymentRequestDto dto) {
        try {
            PaymentIntent intent = stripePaymentService.createPaymentIntent(
                (long) (dto.getAmount() * 100),
                dto.getCurrency().toLowerCase()
            );

            Payment payment = Payment.builder()
                .orderId(dto.getOrderId())
                .userId(dto.getUserId())
                .amount(dto.getAmount())
                .currency(dto.getCurrency())
                .provider("stripe")
                .providerPaymentId(intent.getId())
                .status(PaymentStatus.SUCCESSFUL)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

            return PaymentMapper.toDto(paymentRepository.save(payment));

        } catch (Exception e) {
            throw new RuntimeException("Stripe payment failed: " + e.getMessage(), e);
        }

    }

    @Override
    public PaymentResponseDto getPaymentById(Long id) {
        Payment payment = paymentRepository.findById(id).orElseThrow();
        return PaymentMapper.toDto(payment);
    }

    @Override
    public PaymentResponseDto refundPayment(Long id) {
        Payment payment = paymentRepository.findById(id).orElseThrow();

        if (!"stripe".equalsIgnoreCase(payment.getProvider())) {
            throw new UnsupportedOperationException("Only Stripe payments can be refunded.");
        }

        try {
            RefundCreateParams refundParams = RefundCreateParams.builder()
                .setPaymentIntent(payment.getProviderPaymentId()) // Stripe PaymentIntent ID
                .build();

            Refund.create(refundParams);

            payment.setStatus(PaymentStatus.REFUNDED);
            payment.setUpdatedAt(LocalDateTime.now());
            payment = paymentRepository.save(payment);

            return PaymentMapper.toDto(payment);

        } catch (StripeException e) {
            throw new RuntimeException("Stripe refund failed: " + e.getMessage(), e);
        }
    }
}
