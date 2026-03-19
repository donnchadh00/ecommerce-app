package com.ecommerce.inventory_service.service;

import com.ecommerce.events.order.OrderPlaced;
import com.ecommerce.inventory_service.model.Inventory;
import com.ecommerce.inventory_service.model.InventoryReservation;
import com.ecommerce.inventory_service.repository.InventoryRepository;
import com.ecommerce.inventory_service.repository.InventoryReservationRepository;
import com.ecommerce.inventory_service.service.Impl.InventoryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTests {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private InventoryReservationRepository reservationRepository;

    private InventoryServiceImpl inventoryService;

    @BeforeEach
    void setUp() {
        inventoryService = new InventoryServiceImpl(inventoryRepository, reservationRepository);
    }

    @Test
    void tryReserveMarksReservationsAsReservedWhenStockIsAvailable() {
        var lines = List.of(new OrderPlaced.Line("101", 2));
        var inventory = Inventory.builder().id(1L).productId(101L).quantity(5).build();

        when(reservationRepository.findByOrderId("order-1")).thenReturn(List.of());
        when(inventoryRepository.lockAllByProductIds(List.of(101L))).thenReturn(List.of(inventory));
        when(reservationRepository.findByOrderIdAndProductId("order-1", 101L)).thenReturn(Optional.empty());
        when(reservationRepository.save(any(InventoryReservation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        boolean reserved = inventoryService.tryReserve("order-1", lines);

        assertThat(reserved).isTrue();
        verify(reservationRepository).save(any(InventoryReservation.class));
    }

    @Test
    void confirmConsumesReservedInventoryExactlyOnce() {
        var reservation = reservation("order-2", 101L, 2, "RESERVED");
        var inventory = Inventory.builder().id(1L).productId(101L).quantity(5).build();

        when(reservationRepository.findByOrderId("order-2")).thenReturn(List.of(reservation));
        when(inventoryRepository.lockAllByProductIds(List.of(101L))).thenReturn(List.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(reservationRepository.save(any(InventoryReservation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        inventoryService.confirm("order-2");
        inventoryService.confirm("order-2");

        assertThat(inventory.getQuantity()).isEqualTo(3);
        assertThat(reservation.getStatus()).isEqualTo("CONSUMED");
        verify(inventoryRepository, times(1)).save(inventory);
        verify(reservationRepository, times(1)).save(reservation);
    }

    @Test
    void releaseMarksReservedRowsWithoutChangingStock() {
        var reservation = reservation("order-3", 101L, 1, "RESERVED");

        when(reservationRepository.findByOrderId("order-3")).thenReturn(List.of(reservation));
        when(reservationRepository.save(any(InventoryReservation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        inventoryService.release("order-3");

        assertThat(reservation.getStatus()).isEqualTo("RELEASED");
        verify(reservationRepository).save(reservation);
        verify(inventoryRepository, times(0)).save(any(Inventory.class));
    }

    private InventoryReservation reservation(String orderId, Long productId, int qty, String status) {
        return InventoryReservation.builder()
            .id(UUID.randomUUID())
            .orderId(orderId)
            .productId(productId)
            .qty(qty)
            .status(status)
            .createdAt(Instant.parse("2026-03-19T12:00:00Z"))
            .build();
    }
}
