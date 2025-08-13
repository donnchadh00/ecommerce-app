package com.ecommerce.payment_service.outbox;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface OutboxRepository extends JpaRepository<Outbox, UUID> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select o from Outbox o where o.processedAt is null order by o.createdAt asc")
  List<Outbox> fetchBatchForProcessing(Pageable pageable);

  @Modifying
  @Query("update Outbox o set o.processedAt = CURRENT_TIMESTAMP where o.id in :ids and o.processedAt is null")
  int markProcessed(@Param("ids") List<UUID> ids);
}
