package com.ecommerce.cart_service.model;

import jakarta.persistence.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class CartItem {
    @Id
    @GeneratedValue
    private Long id;

    private Long productId;
    private Integer quantity;
    private Long userId;
}
