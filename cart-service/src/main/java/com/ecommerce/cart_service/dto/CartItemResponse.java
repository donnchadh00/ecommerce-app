package com.ecommerce.cart_service.dto;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CartItemResponse {
    private Long id;
    private Long productId;
    private Integer quantity;
    private Long userId;
}
