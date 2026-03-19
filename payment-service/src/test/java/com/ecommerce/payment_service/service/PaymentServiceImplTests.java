package com.ecommerce.payment_service.service;

import com.ecommerce.payment_service.dto.PaymentRequestDto;
import com.ecommerce.payment_service.model.Payment;
import com.ecommerce.payment_service.model.PaymentStatus;
import com.ecommerce.payment_service.repository.PaymentRepository;
import com.ecommerce.payment_service.service.impl.PaymentServiceImpl;
import com.stripe.model.PaymentIntent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTests {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private StripePaymentService stripePaymentService;

    private PaymentServiceImpl paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentServiceImpl(paymentRepository, stripePaymentService);
    }

    @Test
    void initiatePaymentAuthorizesInsteadOfMarkingSuccessfulImmediately() throws Exception {
        var request = new PaymentRequestDto(101L, 42L, new BigDecimal("129.99"), "EUR");
        var intent = mock(PaymentIntent.class);

        when(paymentRepository.findByOrderId(101L)).thenReturn(Optional.empty());
        when(stripePaymentService.createAuthIntent(eq(12999L), eq("eur"), eq("payment-api-order-101-auth"), eq("101")))
            .thenReturn(intent);
        when(intent.getId()).thenReturn("pi_auth_101");
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            payment.setId(55L);
            return payment;
        });

        var response = paymentService.initiatePayment(request);

        assertThat(response.getStatus()).isEqualTo(PaymentStatus.AUTHORIZED);
        assertThat(response.getProviderPaymentId()).isEqualTo("pi_auth_101");
        assertThat(response.getCurrency()).isEqualTo("EUR");
    }

    @Test
    void initiatePaymentReturnsExistingAuthorizedPaymentWithoutCreatingAnotherStripeIntent() throws Exception {
        var request = new PaymentRequestDto(101L, 42L, new BigDecimal("129.99"), "EUR");
        var existing = Payment.builder()
            .id(55L)
            .orderId(101L)
            .userId(42L)
            .amount(new BigDecimal("129.99"))
            .currency("EUR")
            .provider("stripe")
            .providerPaymentId("pi_existing_101")
            .status(PaymentStatus.AUTHORIZED)
            .createdAt(LocalDateTime.of(2026, 3, 19, 14, 10))
            .updatedAt(LocalDateTime.of(2026, 3, 19, 14, 11))
            .build();

        when(paymentRepository.findByOrderId(101L)).thenReturn(Optional.of(existing));

        var response = paymentService.initiatePayment(request);

        assertThat(response.getId()).isEqualTo(55L);
        assertThat(response.getStatus()).isEqualTo(PaymentStatus.AUTHORIZED);
        verify(stripePaymentService, never()).createAuthIntent(any(), any(), any(), any());
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void refundPaymentVoidsAuthorizedIntentAndMarksPaymentRefunded() throws Exception {
        var authorized = Payment.builder()
            .id(55L)
            .orderId(101L)
            .userId(42L)
            .amount(new BigDecimal("129.99"))
            .currency("EUR")
            .provider("stripe")
            .providerPaymentId("pi_auth_101")
            .status(PaymentStatus.AUTHORIZED)
            .createdAt(LocalDateTime.of(2026, 3, 19, 14, 10))
            .updatedAt(LocalDateTime.of(2026, 3, 19, 14, 11))
            .build();

        when(paymentRepository.findById(55L)).thenReturn(Optional.of(authorized));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = paymentService.refundPayment(55L);

        assertThat(response.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        verify(stripePaymentService).voidPaymentIntent("pi_auth_101");
    }

    @Test
    void refundPaymentRefundsCapturedIntentAndMarksPaymentRefunded() throws Exception {
        var captured = Payment.builder()
            .id(56L)
            .orderId(102L)
            .userId(42L)
            .amount(new BigDecimal("59.99"))
            .currency("EUR")
            .provider("stripe")
            .providerPaymentId("pi_capture_102")
            .status(PaymentStatus.SUCCESSFUL)
            .createdAt(LocalDateTime.of(2026, 3, 19, 14, 10))
            .updatedAt(LocalDateTime.of(2026, 3, 19, 14, 11))
            .build();

        when(paymentRepository.findById(56L)).thenReturn(Optional.of(captured));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = paymentService.refundPayment(56L);

        assertThat(response.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        verify(stripePaymentService).refundCapturedPaymentIntent("pi_capture_102");
    }
}
