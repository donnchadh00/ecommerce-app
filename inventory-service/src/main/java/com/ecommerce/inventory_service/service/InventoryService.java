package com.ecommerce.inventory_service.service;

import com.ecommerce.inventory_service.service.InventoryService;
import com.ecommerce.inventory_service.model.Inventory;
import com.ecommerce.events.order.OrderPlaced;

import java.util.List;

public interface InventoryService {
    Inventory addInventory(Inventory inventory);
    boolean isInStock(Long productId, int quantity);
    void decreaseStock(Long productId, int quantity);
    void increaseStock(Long productId, int quantity);

    // saga-related
    boolean tryReserve(String orderId, List<OrderPlaced.Line> lines);
    void release(String orderId); 
}
