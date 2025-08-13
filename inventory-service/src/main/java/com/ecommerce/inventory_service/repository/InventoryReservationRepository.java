package com.ecommerce.inventory_service.repository;

import com.ecommerce.inventory_service.model.InventoryReservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InventoryReservationRepository extends JpaRepository<InventoryReservation, UUID> {
    Optional<InventoryReservation> findByOrderIdAndProductId(String orderId, Long productId);
    List<InventoryReservation> findByOrderId(String orderId);
}
