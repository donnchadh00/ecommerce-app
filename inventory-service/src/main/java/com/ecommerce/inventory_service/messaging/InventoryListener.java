package com.ecommerce.inventory_service.messaging;

import com.ecommerce.inventory_service.outbox.EventOutbox;
import com.ecommerce.inventory_service.service.InventoryService;

import com.ecommerce.events.Envelope;
import com.ecommerce.events.inventory.InventoryRejected;
import com.ecommerce.events.inventory.InventoryReserved;
import com.ecommerce.events.order.OrderPlaced;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InventoryListener {

    private final InventoryService inventoryService;
    private final EventOutbox outbox;

    @RabbitListener(queues = "inventory.order.placed.q")
    public void onOrderPlaced(Envelope<OrderPlaced> env,
                            @Header(name="traceId", required=false) String traceId) {
        var order = env.data();
        boolean ok = inventoryService.tryReserve(order.orderId(), order.lines());
        if (ok) {
            var items = order.lines().stream()
                .map(l -> new InventoryReserved.Item(l.productId(), l.qty()))
                .toList();
            outbox.save(order.orderId(), "inventory.v1.reserved", new InventoryReserved(order.orderId(), items), traceId);
        } else {
            outbox.save(order.orderId(), "inventory.v1.rejected", new InventoryRejected(order.orderId(), "insufficient_stock"), traceId);
        }
    }

}
