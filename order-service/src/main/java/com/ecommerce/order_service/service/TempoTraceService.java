package com.ecommerce.order_service.service;

import com.ecommerce.order_service.dto.TraceTimelineResponse;
import com.ecommerce.order_service.dto.TraceTimelineSpanResponse;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TempoTraceService {

    private final WebClient.Builder webClientBuilder;

    @Value("${tempo.base-url:http://tempo:3200}")
    private String tempoBaseUrl;

    @Value("${tempo.trace-path:/api/traces/{traceId}}")
    private String tempoTracePath;

    public TraceTimelineResponse fetchTimeline(String traceId) {
        try {
            JsonNode payload = webClientBuilder
                .baseUrl(tempoBaseUrl)
                .build()
                .get()
                .uri(tempoTracePath, traceId)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

            if (payload == null) {
                return TraceTimelineResponse.unavailable(traceId, "Trace is not available yet.");
            }

            return toTimeline(traceId, payload);
        } catch (Exception e) {
            return TraceTimelineResponse.unavailable(traceId, "Trace details are not available in this environment.");
        }
    }

    TraceTimelineResponse toTimeline(String traceId, JsonNode payload) {
        List<SpanRow> rows = new ArrayList<>();

        JsonNode batches = payload.path("batches");
        if (batches.isMissingNode() || !batches.isArray()) {
            return TraceTimelineResponse.unavailable(traceId, "Trace data is still being indexed.");
        }

        for (JsonNode batch : batches) {
            String serviceName = attributeValue(batch.path("resource").path("attributes"), "service.name");
            JsonNode scopeSpans = batch.path("scopeSpans");
            if (!scopeSpans.isArray()) {
                continue;
            }
            for (JsonNode scopeSpan : scopeSpans) {
                JsonNode spans = scopeSpan.path("spans");
                if (!spans.isArray()) {
                    continue;
                }
                for (JsonNode span : spans) {
                    long startNs = longValue(span.path("startTimeUnixNano"));
                    long endNs = longValue(span.path("endTimeUnixNano"));
                    if (startNs <= 0 || endNs <= startNs) {
                        continue;
                    }
                    rows.add(new SpanRow(
                        serviceName == null || serviceName.isBlank() ? "unknown-service" : serviceName,
                        span.path("name").asText("span"),
                        startNs,
                        endNs,
                        spanStatus(span.path("status"))
                    ));
                }
            }
        }

        if (rows.isEmpty()) {
            return TraceTimelineResponse.unavailable(traceId, "Trace data is still being indexed.");
        }

        rows.sort(Comparator.comparingLong(SpanRow::startNs));
        long traceStart = rows.getFirst().startNs();

        List<TraceTimelineSpanResponse> spans = rows.stream()
            .map(row -> new TraceTimelineSpanResponse(
                row.service(),
                row.name(),
                nanosToMillis(row.startNs() - traceStart),
                Math.max(1L, nanosToMillis(row.endNs() - row.startNs())),
                row.status()
            ))
            .toList();

        return new TraceTimelineResponse(traceId, true, null, spans);
    }

    private String attributeValue(JsonNode attributes, String key) {
        if (!attributes.isArray()) {
            return null;
        }
        for (JsonNode attribute : attributes) {
            if (key.equals(attribute.path("key").asText())) {
                JsonNode value = attribute.path("value");
                if (value.hasNonNull("stringValue")) {
                    return value.path("stringValue").asText();
                }
            }
        }
        return null;
    }

    private String spanStatus(JsonNode status) {
        String code = status.path("code").asText();
        return (code == null || code.isBlank()) ? "UNSET" : code;
    }

    private long longValue(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return -1L;
        }
        if (node.isNumber()) {
            return node.asLong();
        }
        try {
            return Long.parseLong(node.asText());
        } catch (NumberFormatException ignored) {
            return -1L;
        }
    }

    private long nanosToMillis(long nanos) {
        return nanos / 1_000_000L;
    }

    private record SpanRow(String service, String name, long startNs, long endNs, String status) {
    }
}
