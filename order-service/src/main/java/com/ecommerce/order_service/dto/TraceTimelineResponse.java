package com.ecommerce.order_service.dto;

import java.util.List;

public record TraceTimelineResponse(
        String traceId,
        boolean available,
        String message,
        List<TraceTimelineSpanResponse> spans
) {
    public static TraceTimelineResponse unavailable(String traceId, String message) {
        return new TraceTimelineResponse(traceId, false, message, List.of());
    }
}
