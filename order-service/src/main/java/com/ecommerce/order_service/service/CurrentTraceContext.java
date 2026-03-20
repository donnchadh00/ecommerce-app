package com.ecommerce.order_service.service;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class CurrentTraceContext {

    public Optional<String> currentTraceId() {
        SpanContext spanContext = Span.current().getSpanContext();
        if (spanContext.isValid()) {
            return Optional.of(spanContext.getTraceId());
        }
        return Optional.empty();
    }
}
