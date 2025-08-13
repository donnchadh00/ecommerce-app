package com.ecommerce.events.payment;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PaymentCaptured(
    String orderId,
    String paymentId,
    BigDecimal amount,
    String currency
) {}
