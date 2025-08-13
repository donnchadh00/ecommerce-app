package com.ecommerce.events.inventory;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record InventoryReserved(
    String orderId,
    List<Item> items
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Item(String productId, int qty) {}
}
