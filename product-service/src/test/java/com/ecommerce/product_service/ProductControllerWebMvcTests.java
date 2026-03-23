package com.ecommerce.product_service;

import com.ecommerce.common.config.JwtAuthFilter;
import com.ecommerce.common.security.JwtService;
import com.ecommerce.product_service.config.SecurityConfig;
import com.ecommerce.product_service.controller.ProductController;
import com.ecommerce.product_service.dto.ProductCreateRequest;
import com.ecommerce.product_service.dto.ProductResponse;
import com.ecommerce.product_service.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(SecurityConfig.class)
class ProductControllerWebMvcTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private JwtAuthFilter jwtAuthFilter;

    @Test
    void getAllProductsReturnsCatalogDtosForAnonymousUsers() throws Exception {
        when(productService.getAll()).thenReturn(List.of(
                new ProductResponse(1L, "Trail Shoes", "Grip for wet ground", new BigDecimal("129.99"), 12)
        ));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Trail Shoes"))
                .andExpect(jsonPath("$[0].quantity").value(12));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createProductAcceptsValidatedWriteDto() throws Exception {
        when(productService.create(any(ProductCreateRequest.class))).thenReturn(
                new ProductResponse(9L, "Desk Lamp", "Warm LED lamp", new BigDecimal("49.99"), 20)
        );

        mockMvc.perform(post("/api/products/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name": "Desk Lamp",
                      "description": "Warm LED lamp",
                      "price": 49.99,
                      "quantity": 20
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(9))
            .andExpect(jsonPath("$.price").value(49.99));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createProductRejectsInvalidPayload() throws Exception {
        mockMvc.perform(post("/api/products/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name": "",
                      "description": "Invalid product",
                      "price": 0,
                      "quantity": -1
                    }
                    """))
            .andExpect(status().isBadRequest());

        verify(productService, never()).create(any(ProductCreateRequest.class));
    }

    @Test
    void getByIdReturnsSummaryDtoForStorefrontConsumers() throws Exception {
        when(productService.getProductById(3L)).thenReturn(Optional.of(
                new ProductService.ProductSummaryDto(3L, "Backpack", "Weatherproof", new BigDecimal("79.99"), "usd", "ACTIVE")
        ));
        when(productService.computeEtag(any(ProductService.ProductSummaryDto.class))).thenReturn("\"etag-3\"");

        mockMvc.perform(get("/api/products/3"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"name\":\"Backpack\"")))
                .andExpect(jsonPath("$.currency").value("usd"));
    }
}
