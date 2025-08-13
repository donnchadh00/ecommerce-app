package com.ecommerce.events.inventory;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record InventoryRejected(
    String orderId,
    String reason
) {}
