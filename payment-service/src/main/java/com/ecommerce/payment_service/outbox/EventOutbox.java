package com.ecommerce.payment_service.outbox;

import com.ecommerce.events.Envelope;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Component
public class EventOutbox {
  private final OutboxRepository repo;
  private final ObjectMapper objectMapper;

  public EventOutbox(OutboxRepository repo, ObjectMapper objectMapper) {
    this.repo = repo;
    this.objectMapper = objectMapper;
  }

  @Transactional
  public void save(String aggregateId, String type, Object data, String traceId) {
    var env = new Envelope<>(
        type,
        UUID.randomUUID().toString(),
        traceId,
        Instant.now(),
        data,
        Map.of()
    );

    var row = new Outbox();
    row.setId(UUID.randomUUID());
    row.setAggregateId(aggregateId);
    row.setType(type);
    row.setTraceId(traceId);
    row.setCreatedAt(Instant.now());
    row.setPayload(objectMapper.valueToTree(env));

    repo.save(row);
  }
}
