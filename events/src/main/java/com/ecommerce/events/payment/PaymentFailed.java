package com.ecommerce.events.payment;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PaymentFailed(
    String orderId,
    String reason
) {}
