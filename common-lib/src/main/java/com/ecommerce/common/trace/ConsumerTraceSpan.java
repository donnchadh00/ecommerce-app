package com.ecommerce.common.trace;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;

import java.util.Map;

public final class ConsumerTraceSpan {

    private static final TextMapGetter<Map<String, String>> GETTER = new TextMapGetter<>() {
        @Override
        public Iterable<String> keys(Map<String, String> carrier) {
            return carrier.keySet();
        }

        @Override
        public String get(Map<String, String> carrier, String key) {
            return carrier.get(key);
        }
    };

    private ConsumerTraceSpan() {
    }

    public static void run(String instrumentationName,
                           String spanName,
                           String destinationName,
                           String traceparent,
                           String traceId,
                           CheckedRunnable runnable) throws Exception {
        Context parentContext = extractedContext(traceparent, traceId);

        Span span = GlobalOpenTelemetry.getTracer(instrumentationName)
            .spanBuilder(spanName)
            .setParent(parentContext)
            .setSpanKind(SpanKind.CONSUMER)
            .startSpan();

        span.setAttribute("messaging.system", "rabbitmq");
        span.setAttribute("messaging.operation", "process");
        span.setAttribute("messaging.destination.name", destinationName);

        try (Scope scope = span.makeCurrent()) {
            runnable.run();
        } catch (Exception ex) {
            span.recordException(ex);
            span.setStatus(StatusCode.ERROR);
            throw ex;
        } finally {
            span.end();
        }
    }

    private static Context extractedContext(String traceparent, String traceId) {
        String effectiveTraceparent = effectiveTraceparent(traceparent, traceId);
        if (effectiveTraceparent == null || effectiveTraceparent.isBlank()) {
            return Context.current();
        }
        return W3CTraceContextPropagator.getInstance()
            .extract(Context.root(), Map.of("traceparent", effectiveTraceparent), GETTER);
    }

    static String effectiveTraceparent(String traceparent, String traceId) {
        // Rabbit observation can attach a traceparent for the scheduled outbox publisher span.
        // Prefer the persisted business trace id so async saga work stays on the order trace.
        if (TraceparentHeader.isValidTraceId(traceId)) {
            return TraceparentHeader.fromTraceId(traceId);
        }
        return traceparent;
    }

    @FunctionalInterface
    public interface CheckedRunnable {
        void run() throws Exception;
    }
}
