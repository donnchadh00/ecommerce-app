package com.ecommerce.inventory_service.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "inventory_reservation",
       indexes = @Index(name="ux_inv_res_order_product", columnList = "order_id,product_id", unique = true))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InventoryReservation {

    @Id
    private UUID id;

    @Column(name = "order_id", nullable = false)
    private String orderId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(nullable = false)
    private Integer qty;

    @Column(nullable = false)
    private String status; // PENDING | RESERVED | RELEASED | REJECTED

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    private void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = Instant.now();
        if (status == null || status.isBlank()) status = "PENDING";
        if (qty == null || qty <= 0) throw new IllegalArgumentException("qty must be > 0");
    }
}
