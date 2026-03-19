package com.ecommerce.payment_service.service.impl;

import com.ecommerce.payment_service.dto.PaymentRequestDto;
import com.ecommerce.payment_service.dto.PaymentResponseDto;
import com.ecommerce.payment_service.mapper.PaymentMapper;
import com.ecommerce.payment_service.model.Payment;
import com.ecommerce.payment_service.model.PaymentStatus;
import com.ecommerce.payment_service.repository.PaymentRepository;
import com.ecommerce.payment_service.service.PaymentService;
import com.ecommerce.payment_service.service.StripePaymentService;
import com.stripe.model.PaymentIntent;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final StripePaymentService stripePaymentService;

    @Override
    public PaymentResponseDto initiatePayment(PaymentRequestDto dto) {
        try {
            Payment existing = paymentRepository.findByOrderId(dto.orderId()).orElse(null);
            if (existing != null && (existing.getStatus() == PaymentStatus.AUTHORIZED || existing.getStatus() == PaymentStatus.SUCCESSFUL)) {
                return PaymentMapper.toDto(existing);
            }

            PaymentIntent intent = stripePaymentService.createAuthIntent(
                dto.amount()
                .movePointRight(2)
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact(),
                dto.currency().toLowerCase(),
                "payment-api-order-" + dto.orderId() + "-auth",
                String.valueOf(dto.orderId())
            );

            LocalDateTime now = LocalDateTime.now();
            Payment payment = existing != null ? existing : Payment.builder()
                .createdAt(now)
                .build();
            payment.setOrderId(dto.orderId());
            payment.setUserId(dto.userId());
            payment.setAmount(dto.amount());
            payment.setCurrency(dto.currency().toUpperCase());
            payment.setProvider("stripe");
            payment.setProviderPaymentId(intent.getId());
            payment.setStatus(PaymentStatus.AUTHORIZED);
            payment.setUpdatedAt(now);
            if (payment.getCreatedAt() == null) {
                payment.setCreatedAt(now);
            }

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
        if (payment.getStatus() == PaymentStatus.REFUNDED) {
            return PaymentMapper.toDto(payment);
        }
        if (payment.getProviderPaymentId() == null || payment.getProviderPaymentId().isBlank()) {
            throw new IllegalStateException("Stripe payment is missing providerPaymentId.");
        }

        try {
            if (payment.getStatus() == PaymentStatus.AUTHORIZED) {
                stripePaymentService.voidPaymentIntent(payment.getProviderPaymentId());
            } else if (payment.getStatus() == PaymentStatus.SUCCESSFUL) {
                stripePaymentService.refundCapturedPaymentIntent(payment.getProviderPaymentId());
            } else {
                throw new IllegalStateException("Only authorized or successful Stripe payments can be reversed.");
            }

            payment.setStatus(PaymentStatus.REFUNDED);
            payment.setUpdatedAt(LocalDateTime.now());
            payment = paymentRepository.save(payment);

            return PaymentMapper.toDto(payment);

        } catch (Exception e) {
            throw new RuntimeException("Stripe reversal failed: " + e.getMessage(), e);
        }
    }
}
