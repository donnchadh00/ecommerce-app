package com.ecommerce.payment_service.service;

import com.stripe.Stripe;
import com.stripe.model.PaymentIntent;
import com.stripe.net.RequestOptions;
import com.stripe.param.PaymentIntentCaptureParams;
import com.stripe.param.PaymentIntentCreateParams;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StripePaymentService {

    @Value("${stripe.secret.key}")
    private String stripeSecretKey;

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeSecretKey;
    }

    public PaymentIntent createAuthIntent(Long amountInCents, String currency,
                                        String idempotencyKey, String orderId) throws Exception {
        var automatic = PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
            .setEnabled(true)
            .setAllowRedirects(PaymentIntentCreateParams.AutomaticPaymentMethods.AllowRedirects.NEVER)
            .build();

        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
            .setAmount(amountInCents)
            .setCurrency(currency.toLowerCase())
            .setCaptureMethod(PaymentIntentCreateParams.CaptureMethod.MANUAL) // authorize only
            .setPaymentMethod("pm_card_visa")
            .setConfirm(true)
            .setAutomaticPaymentMethods(automatic)
            .putMetadata("orderId", orderId)
            .build();

        RequestOptions opts = RequestOptions.builder()
            .setIdempotencyKey(idempotencyKey)
            .build();

        return PaymentIntent.create(params, opts);
    }

    public PaymentIntent capturePaymentIntent(String paymentIntentId) throws Exception {
        PaymentIntent pi = PaymentIntent.retrieve(paymentIntentId);
        return pi.capture(PaymentIntentCaptureParams.builder().build());
    }

    public PaymentIntent createPaymentIntent(Long amountInCents, String currency) throws Exception {
        PaymentIntentCreateParams.AutomaticPaymentMethods automaticPaymentMethods =
            PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                .setEnabled(true)
                .setAllowRedirects(PaymentIntentCreateParams.AutomaticPaymentMethods.AllowRedirects.NEVER)
                .build();

        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
            .setAmount(amountInCents)
            .setCurrency(currency.toLowerCase())
            .setPaymentMethod("pm_card_visa")  // test method
            .setConfirm(true)
            .setAutomaticPaymentMethods(automaticPaymentMethods)
            .build();

        return PaymentIntent.create(params);
    }
}
