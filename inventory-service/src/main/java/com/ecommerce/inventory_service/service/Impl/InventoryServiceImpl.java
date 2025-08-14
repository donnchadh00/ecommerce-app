package com.ecommerce.inventory_service.service.Impl;

import com.ecommerce.inventory_service.model.Inventory;
import com.ecommerce.inventory_service.repository.InventoryRepository;
import com.ecommerce.inventory_service.service.InventoryService;
import com.ecommerce.inventory_service.model.InventoryReservation;
import com.ecommerce.inventory_service.repository.InventoryReservationRepository;

import com.ecommerce.events.order.OrderPlaced;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.UUID;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;
    private final InventoryReservationRepository reservationRepository;

    @Override
    public Inventory addInventory(Inventory inventory) {
        return inventoryRepository.save(inventory);
    }

    @Override
    public boolean isInStock(Long productId, int quantity) {
        return inventoryRepository.findByProductId(productId)
            .map(inventory -> inventory.getQuantity() >= quantity)
            .orElse(false);
    }

    @Override
    public void decreaseStock(Long productId, int quantity) {
        Inventory inventory = inventoryRepository.findByProductId(productId)
            .orElseThrow(() -> new RuntimeException("Product not found in inventory"));
        inventory.setQuantity(inventory.getQuantity() - quantity);
        inventoryRepository.save(inventory);
    }

    @Override
    public void increaseStock(Long productId, int quantity) {
        Inventory inventory = inventoryRepository.findByProductId(productId)
            .orElseThrow(() -> new RuntimeException("Product not found in inventory"));
        inventory.setQuantity(inventory.getQuantity() + quantity);
        inventoryRepository.save(inventory);
    }


    // Saga methods

    // Reserve all items for an order atomically. Idempotent across retries.
    @Override
    @Transactional
    public boolean tryReserve(String orderId, List<OrderPlaced.Line> lines) {
        // Idempotency: if we already reserved everything, no-op; if rejected before, return false
        var existing = reservationRepository.findByOrderId(orderId);
        if (!existing.isEmpty() && existing.stream().allMatch(r -> "RESERVED".equals(r.getStatus()))) {
            return true;
        }
        if (!existing.isEmpty() && existing.stream().anyMatch(r -> "REJECTED".equals(r.getStatus()))) {
            return false;
        }

        // Lock inventory rows we need
        var productIds = lines.stream()
                .map(l -> Long.valueOf(l.productId()))
                .toList();
        var stocks = inventoryRepository.lockAllByProductIds(productIds);
        var stockByProduct = new HashMap<Long, Inventory>();
        stocks.forEach(s -> stockByProduct.put(s.getProductId(), s));

        // Validate availability
        for (var l : lines) {
            long pid = Long.parseLong(l.productId());
            var inv = stockByProduct.get(pid);
            if (inv == null || inv.getQuantity() < l.qty()) {
                // Mark REJECTED reservations idempotently
                for (var each : lines) upsertReservation(orderId, Long.valueOf(each.productId()), each.qty(), "REJECTED");
                return false;
            }
        }

        // Mark as RESERVED (we are not decrementing here yet)
        for (var l : lines) {
            upsertReservation(orderId, Long.valueOf(l.productId()), l.qty(), "RESERVED");
        }
        return true;
    }

    @Override
    @Transactional
    public void release(String orderId) {
        var reservations = reservationRepository.findByOrderId(orderId);
        if (reservations.isEmpty()) return;

        for (var r : reservations) {
            if (!"RESERVED".equals(r.getStatus())) continue;
            r.setStatus("RELEASED");
            reservationRepository.save(r);
        }
    }

    private void upsertReservation(String orderId, Long productId, int qty, String status) {
        var res = reservationRepository.findByOrderIdAndProductId(orderId, productId).orElseGet(() ->
            InventoryReservation.builder()
                .id(UUID.randomUUID())
                .orderId(orderId)
                .productId(productId)
                .qty(qty)
                .status("PENDING")
                .build()
        );
        res.setQty(qty);
        res.setStatus(status);
        reservationRepository.save(res);
    }
}
