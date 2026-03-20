package com.ecommerce.common.trace;

import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

public final class TraceparentHeader {

    private static final Pattern TRACE_ID_PATTERN = Pattern.compile("^[0-9a-f]{32}$");

    private TraceparentHeader() {
    }

    public static String fromTraceId(String traceId) {
        if (!isValidTraceId(traceId)) {
            return null;
        }
        return "00-" + traceId + "-" + randomSpanId() + "-01";
    }

    public static boolean isValidTraceId(String traceId) {
        return traceId != null && TRACE_ID_PATTERN.matcher(traceId).matches();
    }

    private static String randomSpanId() {
        long value = ThreadLocalRandom.current().nextLong();
        if (value == Long.MIN_VALUE) {
            value = 0L;
        }
        return String.format("%016x", Math.abs(value));
    }
}
