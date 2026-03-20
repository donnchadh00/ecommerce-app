package com.ecommerce.common.trace;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TraceparentHeaderTests {

    @Test
    void buildsW3cTraceparentFromValidTraceId() {
        String traceId = "7af124fbb76349098d5d4b82680293c5";

        String header = TraceparentHeader.fromTraceId(traceId);

        assertNotNull(header);
        assertTrue(header.matches("^00-" + traceId + "-[0-9a-f]{16}-01$"));
    }

    @Test
    void rejectsInvalidTraceIds() {
        assertNull(TraceparentHeader.fromTraceId(null));
        assertNull(TraceparentHeader.fromTraceId("trace-123"));
        assertNull(TraceparentHeader.fromTraceId("7af124fbb76349098d5d4b82680293c"));
        assertFalse(TraceparentHeader.isValidTraceId("not-hex"));
    }
}
