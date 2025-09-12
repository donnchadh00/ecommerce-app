package com.ecommerce.cart_service.service;

import com.ecommerce.cart_service.repository.CartItemRepository;
import com.ecommerce.cart_service.dto.CartItemRequest;
import com.ecommerce.cart_service.dto.CartItemResponse;
import com.ecommerce.cart_service.mapper.CartItemMapper;
import com.ecommerce.cart_service.model.CartItem;
import com.ecommerce.cart_service.client.ProductClient;
import lombok.RequiredArgsConstructor;

import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CartService {
    private final CartItemRepository cartRepo;
    private final ProductClient productClient;

    @Transactional
    public CartItemResponse addItemToCart(Long userId, CartItemRequest request) {
        var productOpt = productClient.getProduct(request.getProductId());
        if (productOpt.isEmpty()) {
            throw new IllegalArgumentException("Product not found");
        }
        int tries = 0;
        while (true) {
        try {        
            var existing = cartRepo.findByUserIdAndProductId(userId, request.getProductId());
            CartItem entity = existing.map(ci -> {
                int current = ci.getQuantity() == null ? 0 : ci.getQuantity();
                ci.setQuantity(current + request.getQuantity());
                return ci;
            }).orElseGet(() -> {
                request.setUserId(userId);
                return CartItemMapper.toEntity(request);
            });
            CartItem saved = cartRepo.save(entity);
            return CartItemMapper.toResponse(saved);
        } catch (DataAccessException e) {
            if (++tries <= 2) {
                try {
                    Thread.sleep(100L * tries);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(ie);
                }
            } else {
                throw e;
            }
        }
        }
    }

    @Transactional
    public List<CartItemResponse> getCartItemsByUser(Long userId) {
        List<CartItem> items = cartRepo.findByUserId(userId);
        List<CartItemResponse> responses = new ArrayList<>();

        for (CartItem item : items) {
            responses.add(CartItemMapper.toResponse(item));
        }
        return responses;
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
