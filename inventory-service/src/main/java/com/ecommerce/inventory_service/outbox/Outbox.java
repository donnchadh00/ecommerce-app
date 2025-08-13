package com.ecommerce.inventory_service.outbox;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox")
@Getter @Setter @NoArgsConstructor
public class Outbox {

  @Id
  private UUID id;

  @Column(name = "aggregate_id", nullable = false)
  private String aggregateId;

  @Column(nullable = false)
  private String type;

  @Column(name = "trace_id")
  private String traceId;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "processed_at")
  private Instant processedAt;

  @JdbcTypeCode(SqlTypes.JSON)                // <-- tell Hibernate it’s JSON
  @Column(columnDefinition = "jsonb")         // <-- match DB column type
  private JsonNode payload;                   // or Map<String,Object> / Object
}
