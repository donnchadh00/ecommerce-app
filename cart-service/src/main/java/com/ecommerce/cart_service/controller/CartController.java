package com.ecommerce.cart_service.controller;

import com.ecommerce.cart_service.dto.CartItemRequest;
import com.ecommerce.cart_service.dto.CartItemResponse;
import com.ecommerce.cart_service.service.CartService;
import com.ecommerce.common.security.JwtService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;


@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;
    private final JwtService jwtService;

    @PostMapping("/add")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<CartItemResponse> addToCart(@RequestBody CartItemRequest requestDto, HttpServletRequest request) {
        
        Long userId = jwtService.extractUserId(request.getHeader("Authorization"));
        CartItemResponse response = cartService.addItemToCart(userId, requestDto);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public List<CartItemResponse> getCart(HttpServletRequest request) {
        Long userId = jwtService.extractUserId(request.getHeader("Authorization"));
        return cartService.getCartItemsByUser(userId);
    }

    @DeleteMapping("/remove")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<String> removeFromCart(@RequestParam Long productId, HttpServletRequest request) {
        Long userId = jwtService.extractUserId(request.getHeader("Authorization"));
        cartService.removeItem(userId, productId);
        return ResponseEntity.ok("Item removed");
    }

    @DeleteMapping("/clear")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<String> removeFromCart(HttpServletRequest request) {
        Long userId = jwtService.extractUserId(request.getHeader("Authorization"));
        cartService.clearCart(userId);
        return ResponseEntity.ok("Cart cleared");
    }
}
