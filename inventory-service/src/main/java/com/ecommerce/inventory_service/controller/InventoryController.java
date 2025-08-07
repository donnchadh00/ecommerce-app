package com.ecommerce.inventory_service.controller;

import com.ecommerce.inventory_service.service.InventoryService;
import com.ecommerce.inventory_service.model.Inventory;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/add")
    public ResponseEntity<Inventory> addInventory(@RequestBody Inventory inventory) {
        Inventory savedInventory = inventoryService.addInventory(inventory);
        return ResponseEntity.ok(savedInventory);
    }

    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @GetMapping("/check")
    public ResponseEntity<Boolean> isInStock(
        @RequestParam Long productId,
        @RequestParam int quantity
    ) {
        return ResponseEntity.ok(inventoryService.isInStock(productId, quantity));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/decrease")
    public ResponseEntity<Void> decreaseStock(
        @RequestParam Long productId,
        @RequestParam int quantity
    ) {
        inventoryService.decreaseStock(productId, quantity);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/increase")
    public ResponseEntity<Void> increaseStock(
        @RequestParam Long productId,
        @RequestParam int quantity
    ) {
        inventoryService.increaseStock(productId, quantity);
        return ResponseEntity.ok().build();
    }
}
