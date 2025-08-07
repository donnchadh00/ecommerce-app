package com.ecommerce.cart_service.controller;

import com.ecommerce.cart_service.model.CartItem;
import com.ecommerce.cart_service.repository.CartItemRepository;
import com.ecommerce.cart_service.service.CartService;
import com.ecommerce.common.security.JwtService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;


@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartItemRepository cartRepo;
    private final CartService cartService;
    private final JwtService jwtService;

    @PostMapping("/add")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> addToCart(@RequestBody CartItem item, HttpServletRequest request) {
        Long userId = jwtService.extractUserId(request.getHeader("Authorization"));
        cartService.addItemToCart(userId, item);
        return ResponseEntity.ok("Item added");
    }

    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public List<CartItem> getCart(HttpServletRequest request) {
        Long userId = jwtService.extractUserId(request.getHeader("Authorization"));
        return cartService.getCartItemsByUser(userId);
    }

    @DeleteMapping("/remove")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> removeFromCart(@RequestParam Long productId, HttpServletRequest request) {
        Long userId = jwtService.extractUserId(request.getHeader("Authorization"));
        cartService.removeItem(userId, productId);
        return ResponseEntity.ok("Item removed");
    }

    @DeleteMapping("/clear")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> removeFromCart(HttpServletRequest request) {
        Long userId = jwtService.extractUserId(request.getHeader("Authorization"));
        cartService.clearCart(userId);
        return ResponseEntity.ok("Cart cleared");
    }
}
