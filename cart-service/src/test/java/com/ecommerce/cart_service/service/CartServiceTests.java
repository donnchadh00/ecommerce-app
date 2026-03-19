package com.ecommerce.cart_service.service;

import com.ecommerce.cart_service.client.ProductClient;
import com.ecommerce.cart_service.dto.CartItemRequest;
import com.ecommerce.cart_service.dto.CartItemResponse;
import com.ecommerce.cart_service.model.CartItem;
import com.ecommerce.cart_service.repository.CartItemRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartServiceTests {

    @Mock
    private CartItemRepository cartRepo;

    @Mock
    private ProductClient productClient;

    @InjectMocks
    private CartService cartService;

    @Test
    void addItemToCartCreatesNewCartEntryForExistingProduct() {
        CartItemRequest request = new CartItemRequest();
        request.setProductId(5L);
        request.setQuantity(2);

        CartItem saved = new CartItem(11L, 5L, 2, 44L);

        when(productClient.getProduct(5L)).thenReturn(Optional.of(
            new ProductClient.ProductDto(5L, "Trail Shoes", "EUR", "ACTIVE", "SKU-5", 12999L)
        ));
        when(cartRepo.findByUserIdAndProductId(44L, 5L)).thenReturn(Optional.empty());
        when(cartRepo.save(any(CartItem.class))).thenReturn(saved);

        CartItemResponse response = cartService.addItemToCart(44L, request);

        assertThat(response.getId()).isEqualTo(11L);
        assertThat(response.getProductId()).isEqualTo(5L);
        assertThat(response.getQuantity()).isEqualTo(2);
        assertThat(response.getUserId()).isEqualTo(44L);
    }

    @Test
    void addItemToCartIncrementsQuantityWhenItemAlreadyExists() {
        CartItemRequest request = new CartItemRequest();
        request.setProductId(8L);
        request.setQuantity(3);

        CartItem existing = new CartItem(9L, 8L, 1, 12L);
        CartItem updated = new CartItem(9L, 8L, 4, 12L);

        when(productClient.getProduct(8L)).thenReturn(Optional.of(
            new ProductClient.ProductDto(8L, "Desk Lamp", "EUR", "ACTIVE", "SKU-8", 4999L)
        ));
        when(cartRepo.findByUserIdAndProductId(12L, 8L)).thenReturn(Optional.of(existing));
        when(cartRepo.save(existing)).thenReturn(updated);

        CartItemResponse response = cartService.addItemToCart(12L, request);

        assertThat(response.getQuantity()).isEqualTo(4);
        verify(cartRepo).save(existing);
    }

    @Test
    void addItemToCartRetriesTransientRepositoryFailures() {
        CartItemRequest request = new CartItemRequest();
        request.setProductId(3L);
        request.setQuantity(1);

        CartItem saved = new CartItem(5L, 3L, 1, 77L);

        when(productClient.getProduct(3L)).thenReturn(Optional.of(
            new ProductClient.ProductDto(3L, "Backpack", "EUR", "ACTIVE", "SKU-3", 7999L)
        ));
        when(cartRepo.findByUserIdAndProductId(77L, 3L)).thenReturn(Optional.empty());
        when(cartRepo.save(any(CartItem.class)))
            .thenThrow(new DataAccessResourceFailureException("temporary failure"))
            .thenReturn(saved);

        CartItemResponse response = cartService.addItemToCart(77L, request);

        assertThat(response.getId()).isEqualTo(5L);
        verify(cartRepo, times(2)).save(any(CartItem.class));
    }

    @Test
    void addItemToCartRejectsUnknownProducts() {
        CartItemRequest request = new CartItemRequest();
        request.setProductId(404L);
        request.setQuantity(1);

        when(productClient.getProduct(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.addItemToCart(15L, request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Product not found");
    }
}
