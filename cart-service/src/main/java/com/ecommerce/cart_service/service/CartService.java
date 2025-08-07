package com.ecommerce.cart_service.service;

import com.ecommerce.cart_service.repository.CartItemRepository;
import com.ecommerce.cart_service.model.CartItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CartService {
    private final CartItemRepository cartRepo;

    @Transactional
    public void addItemToCart(Long userId, CartItem item) {
        item.setUserId(userId);
        cartRepo.save(item);
    }

    @Transactional
    public List<CartItem> getCartItemsByUser(Long userId) {
        return cartRepo.findByUserId(userId);
    }

    @Transactional
    public void removeItem(Long userId, Long productId) {
        cartRepo.deleteByUserIdAndProductId(userId, productId);
    }

    @Transactional
    public void clearCart(Long userId) {
        cartRepo.deleteByUserId(userId);
    }
}
