package com.ecommerce.order_service.service;

import com.ecommerce.order_service.dto.TraceTimelineResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;

class TempoTraceServiceTests {

    private TempoTraceService tempoTraceService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        tempoTraceService = new TempoTraceService(WebClient.builder());
    }

    @Test
    void toTimelineBuildsWaterfallRowsFromTempoPayload() throws Exception {
        String payload = """
            {
              "batches": [
                {
                  "resource": {
                    "attributes": [
                      { "key": "service.name", "value": { "stringValue": "order-service" } }
                    ]
                  },
                  "scopeSpans": [
                    {
                      "spans": [
                        {
                          "name": "POST /api/orders",
                          "startTimeUnixNano": "1000000000",
                          "endTimeUnixNano": "1050000000",
                          "status": { "code": "STATUS_CODE_OK" }
                        }
                      ]
                    }
                  ]
                },
                {
                  "resource": {
                    "attributes": [
                      { "key": "service.name", "value": { "stringValue": "inventory-service" } }
                    ]
                  },
                  "scopeSpans": [
                    {
                      "spans": [
                        {
                          "name": "inventory reserve",
                          "startTimeUnixNano": "1060000000",
                          "endTimeUnixNano": "1100000000",
                          "status": { "code": "STATUS_CODE_OK" }
                        }
                      ]
                    }
                  ]
                }
              ]
            }
            """;

        TraceTimelineResponse timeline = tempoTraceService.toTimeline("trace-123", objectMapper.readTree(payload));

        assertThat(timeline.available()).isTrue();
        assertThat(timeline.spans()).hasSize(2);
        assertThat(timeline.spans().get(0).service()).isEqualTo("order-service");
        assertThat(timeline.spans().get(0).offsetMs()).isEqualTo(0);
        assertThat(timeline.spans().get(0).durationMs()).isEqualTo(50);
        assertThat(timeline.spans().get(1).service()).isEqualTo("inventory-service");
        assertThat(timeline.spans().get(1).offsetMs()).isEqualTo(60);
    }

    @Test
    void toTimelineReturnsUnavailableWhenNoSpansExist() throws Exception {
        TraceTimelineResponse timeline = tempoTraceService.toTimeline("trace-123", objectMapper.readTree("""
            { "batches": [] }
            """));

        assertThat(timeline.available()).isFalse();
        assertThat(timeline.message()).contains("indexed");
        assertThat(timeline.spans()).isEmpty();
    }
}
