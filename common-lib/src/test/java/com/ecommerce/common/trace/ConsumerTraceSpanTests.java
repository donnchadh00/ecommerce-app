package com.ecommerce.common.trace;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConsumerTraceSpanTests {

    @Test
    void prefersBusinessTraceIdOverIncomingTraceparent() {
        String traceId = "591c6bfecdbf8fc2337f047518b55991";
        String otherTraceparent = "00-aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa-bbbbbbbbbbbbbbbb-01";

        String effective = ConsumerTraceSpan.effectiveTraceparent(otherTraceparent, traceId);

        assertEquals(traceId, effective.split("-")[1]);
    }

    @Test
    void fallsBackToIncomingTraceparentWhenBusinessTraceIdIsUnavailable() {
        String traceparent = "00-aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa-bbbbbbbbbbbbbbbb-01";

        String effective = ConsumerTraceSpan.effectiveTraceparent(traceparent, null);

        assertEquals(traceparent, effective);
    }
}
