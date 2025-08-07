package com.ecommerce.payment_service.service.impl;

import com.ecommerce.payment_service.dto.PaymentRequestDto;
import com.ecommerce.payment_service.dto.PaymentResponseDto;
import com.ecommerce.payment_service.mapper.PaymentMapper;
import com.ecommerce.payment_service.model.Payment;
import com.ecommerce.payment_service.model.PaymentStatus;
import com.ecommerce.payment_service.repository.PaymentRepository;
import com.ecommerce.payment_service.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;

    @Override
    public PaymentResponseDto initiatePayment(PaymentRequestDto dto) {
        Payment payment = PaymentMapper.toEntity(dto);
        payment = paymentRepository.save(payment);
        return PaymentMapper.toDto(payment);
    }

    @Override
    public PaymentResponseDto getPaymentById(Long id) {
        Payment payment = paymentRepository.findById(id).orElseThrow();
        return PaymentMapper.toDto(payment);
    }

    @Override
    public PaymentResponseDto refundPayment(Long id) {
        Payment payment = paymentRepository.findById(id).orElseThrow();
        payment.setStatus(PaymentStatus.REFUNDED);
        payment.setUpdatedAt(LocalDateTime.now());
        payment = paymentRepository.save(payment);
        return PaymentMapper.toDto(payment);
    }
}
