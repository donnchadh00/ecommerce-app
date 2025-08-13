package com.ecommerce.events.order;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OrderPlaced(
    String orderId,
    String userId,
    @JsonAlias("items") List<Line> lines,
    BigDecimal total
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Line(
        String productId, 
        @JsonProperty("qty") @JsonAlias("quantity") int qty
    ) {}
}
