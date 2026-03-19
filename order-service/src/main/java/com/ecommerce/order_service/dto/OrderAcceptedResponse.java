package com.ecommerce.order_service.dto;

public record OrderAcceptedResponse(
        Long orderId,
        String status,
        String traceId
) {
}
