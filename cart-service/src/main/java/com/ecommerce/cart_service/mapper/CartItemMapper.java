package com.ecommerce.cart_service.mapper;

import com.ecommerce.cart_service.dto.CartItemRequest;
import com.ecommerce.cart_service.dto.CartItemResponse;
import com.ecommerce.cart_service.model.CartItem;

public class CartItemMapper {

    public static CartItem toEntity(CartItemRequest dto) {
        CartItem e = new CartItem();
        e.setUserId(dto.getUserId());
        e.setProductId(dto.getProductId());
        e.setQuantity(dto.getQuantity());
        return e;
    }

    public static CartItemResponse toResponse(CartItem e) {
        return new CartItemResponse(
                e.getId(),
                e.getProductId(),
                e.getQuantity(),
                e.getUserId()
        );
    }
}
