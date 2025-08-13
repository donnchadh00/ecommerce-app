package com.ecommerce.events;

import java.time.Instant;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Envelope<T>(
    String type,          // e.g., "order.v1.placed"
    String id,            // UUID for this event
    String traceId,       // propagate for tracing
    Instant occurredAt,
    T data,
    Map<String, String> meta
) {}
