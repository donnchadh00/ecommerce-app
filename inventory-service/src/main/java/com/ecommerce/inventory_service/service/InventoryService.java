package com.ecommerce.inventory_service.service;

import com.ecommerce.inventory_service.service.InventoryService;
import com.ecommerce.inventory_service.model.Inventory;

public interface InventoryService {
    Inventory addInventory(Inventory inventory);
    boolean isInStock(Long productId, int quantity);
    void decreaseStock(Long productId, int quantity);
    void increaseStock(Long productId, int quantity);
}
