package com.ecommerce.payment_service.outbox; 

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ecommerce.payment_service.messaging.RabbitConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Component
@Slf4j
public class OutboxPublisher {
  private final OutboxRepository outboxRepository;
  private final RabbitTemplate rabbitTemplate;
  private final int batchSize = 10;

  @Scheduled(fixedDelay = 200)
  @Transactional
  public void publishBatch() {
    var rows = outboxRepository.fetchBatchForProcessing(PageRequest.of(0, batchSize));
    if (rows.isEmpty()) return;

    List<UUID> success = new ArrayList<>();
    for (var o : rows) {
      try {
        rabbitTemplate.convertAndSend(
            RabbitConfig.EXCHANGE,
            o.getType(),
            o.getPayload(), // <-- send JsonNode/Object directly
            m -> {
              if (o.getTraceId() != null) {
                m.getMessageProperties().setHeader("traceId", o.getTraceId());
              }
              return m;
            }
        );
        success.add(o.getId());
      } catch (Exception ex) {
        log.error("Outbox publish failed id={} type={}", o.getId(), o.getType(), ex);
      }
    }
    if (!success.isEmpty()) outboxRepository.markProcessed(success);
  }
}
