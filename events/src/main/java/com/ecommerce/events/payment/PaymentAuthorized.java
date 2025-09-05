package com.ecommerce.events.payment;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PaymentAuthorized(
    String orderId,
    String providerPaymentId,
    BigDecimal amount,
    String currency
) {}
