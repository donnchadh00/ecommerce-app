package com.ecommerce.inventory_service.messaging;

import com.ecommerce.events.Envelope;
import com.ecommerce.events.inventory.InventoryRejected;
import com.ecommerce.events.inventory.InventoryReserved;
import com.ecommerce.events.order.OrderCancelled;
import com.ecommerce.events.order.OrderConfirmed;
import com.ecommerce.events.order.OrderPlaced;
import com.ecommerce.inventory_service.outbox.EventOutbox;
import com.ecommerce.inventory_service.service.InventoryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryListenerTests {

    @Mock
    private InventoryService inventoryService;

    @Mock
    private EventOutbox outbox;

    @InjectMocks
    private InventoryListener inventoryListener;

    @Test
    void orderPlacedPublishesReservedEventWhenReservationSucceeds() throws Exception {
        var order = new OrderPlaced("trace-1", "5", List.of(new OrderPlaced.Line("101", 2)), new BigDecimal("49.99"));
        when(inventoryService.tryReserve(eq("trace-1"), eq(order.lines()))).thenReturn(true);

        inventoryListener.onOrderPlaced(envelope("order.v1.placed", order, "trace-123"), "trace-123", null);

        verify(outbox).save(eq("trace-1"), eq("inventory.v1.reserved"), eq(new InventoryReserved("trace-1",
            List.of(new InventoryReserved.Item("101", 2)))), eq("trace-123"));
    }

    @Test
    void orderConfirmedConsumesReservedInventory() throws Exception {
        inventoryListener.onOrderConfirmed(envelope("order.v1.confirmed", new OrderConfirmed("77"), "trace-456"), "trace-456", null);

        verify(inventoryService).confirm("77");
    }

    @Test
    void orderCancelledReleasesReservation() throws Exception {
        inventoryListener.onOrderCancelled(envelope("order.v1.cancelled", new OrderCancelled("88", "payment_failed"), "trace-789"), "trace-789", null);

        verify(inventoryService).release("88");
    }

    @Test
    void orderPlacedPublishesRejectedEventWhenReservationFails() throws Exception {
        var order = new OrderPlaced("trace-2", "9", List.of(new OrderPlaced.Line("101", 4)), new BigDecimal("19.99"));
        when(inventoryService.tryReserve(eq("trace-2"), eq(order.lines()))).thenReturn(false);

        inventoryListener.onOrderPlaced(envelope("order.v1.placed", order, "trace-999"), "trace-999", null);

        verify(outbox).save(eq("trace-2"), eq("inventory.v1.rejected"),
            eq(new InventoryRejected("trace-2", "insufficient_stock")), eq("trace-999"));
    }

    private <T> Envelope<T> envelope(String type, T data, String traceId) {
        return new Envelope<>(
            type,
            UUID.randomUUID().toString(),
            traceId,
            Instant.parse("2026-03-19T14:00:00Z"),
            data,
            Map.of()
        );
    }
}
