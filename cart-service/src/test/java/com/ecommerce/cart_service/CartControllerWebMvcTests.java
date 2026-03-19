package com.ecommerce.cart_service;

import com.ecommerce.cart_service.config.SecurityConfig;
import com.ecommerce.cart_service.controller.CartController;
import com.ecommerce.cart_service.dto.CartItemRequest;
import com.ecommerce.cart_service.dto.CartItemResponse;
import com.ecommerce.cart_service.service.CartService;
import com.ecommerce.common.config.JwtAuthFilter;
import com.ecommerce.common.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CartController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(SecurityConfig.class)
class CartControllerWebMvcTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CartService cartService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private JwtAuthFilter jwtAuthFilter;

    @Test
    @WithMockUser(roles = "ADMIN")
    void addToCartUsesUserIdFromJwtHeader() throws Exception {
        CartItemRequest request = new CartItemRequest();
        request.setProductId(7L);
        request.setQuantity(2);

        CartItemResponse response = new CartItemResponse(1L, 7L, 2, 99L);

        when(jwtService.extractUserId("Bearer demo-token")).thenReturn(99L);
        when(cartService.addItemToCart(eq(99L), any(CartItemRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/cart/add")
                .header("Authorization", "Bearer demo-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.productId").value(7))
            .andExpect(jsonPath("$.quantity").value(2))
            .andExpect(jsonPath("$.userId").value(99));
    }

    @Test
    @WithMockUser(roles = "USER")
    void getCartReturnsCurrentUsersItems() throws Exception {
        when(jwtService.extractUserId("Bearer demo-token")).thenReturn(21L);
        when(cartService.getCartItemsByUser(21L)).thenReturn(List.of(
            new CartItemResponse(1L, 5L, 1, 21L),
            new CartItemResponse(2L, 6L, 3, 21L)
        ));

        mockMvc.perform(get("/api/cart")
                .header("Authorization", "Bearer demo-token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].productId").value(5))
            .andExpect(jsonPath("$[1].quantity").value(3));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void clearCartAllowsAdminRoleForSeededDemoFlow() throws Exception {
        when(jwtService.extractUserId("Bearer admin-token")).thenReturn(10L);

        mockMvc.perform(delete("/api/cart/clear")
                .header("Authorization", "Bearer admin-token"))
            .andExpect(status().isOk())
            .andExpect(content().string("Cart cleared"));

        verify(cartService).clearCart(10L);
    }
}
