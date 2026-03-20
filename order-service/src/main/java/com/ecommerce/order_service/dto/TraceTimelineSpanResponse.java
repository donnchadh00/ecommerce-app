package com.ecommerce.order_service.dto;

public record TraceTimelineSpanResponse(
        String service,
        String name,
        long offsetMs,
        long durationMs,
        String status
) {
}
