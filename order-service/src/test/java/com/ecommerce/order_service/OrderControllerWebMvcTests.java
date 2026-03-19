package com.ecommerce.order_service;

import com.ecommerce.common.config.JwtAuthFilter;
import com.ecommerce.common.security.JwtService;
import com.ecommerce.order_service.config.SecurityConfig;
import com.ecommerce.order_service.controller.OrderController;
import com.ecommerce.order_service.dto.OrderCreateRequest;
import com.ecommerce.order_service.dto.OrderResponse;
import com.ecommerce.order_service.service.OrderService;
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
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(SecurityConfig.class)
class OrderControllerWebMvcTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private JwtAuthFilter jwtAuthFilter;

    @Test
    @WithMockUser(roles = "USER")
    void placeOrderAcceptsValidatedRequestDto() throws Exception {
        when(orderService.createOrder(any(OrderCreateRequest.class), any(), eq("trace-123"))).thenReturn(
                new OrderResponse(
                        101L,
                        42L,
                        LocalDateTime.of(2026, 3, 19, 13, 0),
                        "PENDING",
                        new BigDecimal("129.99"),
                        List.of(new OrderResponse.OrderItemResponse(1L, 7L, 2, new BigDecimal("65.00")))
                )
        );

        mockMvc.perform(post("/api/orders")
                .header("X-Trace-Id", "trace-123")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "items": [
                        { "productId": 7, "quantity": 2 }
                      ]
                    }
                    """))
            .andExpect(status().isAccepted())
            .andExpect(header().string("X-Trace-Id", "trace-123"))
            .andExpect(header().string("Location", "/api/orders/101"))
            .andExpect(jsonPath("$.orderId").value(101))
            .andExpect(jsonPath("$.status").value("PENDING"))
            .andExpect(jsonPath("$.traceId").value("trace-123"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void placeOrderRejectsInvalidItems() throws Exception {
        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "items": [
                        { "productId": 7, "quantity": 0 }
                      ]
                    }
                    """))
            .andExpect(status().isBadRequest());

        verify(orderService, never()).createOrder(any(), any(), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getUserOrdersAllowsAdminForDemoFlow() throws Exception {
        when(orderService.getOrdersForUser(any())).thenReturn(List.of(
                new OrderResponse(
                        11L,
                        99L,
                        LocalDateTime.of(2026, 3, 19, 10, 15),
                        "CONFIRMED",
                        new BigDecimal("49.99"),
                        List.of(new OrderResponse.OrderItemResponse(5L, 3L, 1, new BigDecimal("49.99")))
                )
        ));

        mockMvc.perform(get("/api/orders"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(11))
            .andExpect(jsonPath("$[0].items[0].productId").value(3));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAllOrdersReturnsResponseDtos() throws Exception {
        when(orderService.getAllOrders()).thenReturn(List.of(
                new OrderResponse(
                        21L,
                        5L,
                        LocalDateTime.of(2026, 3, 19, 9, 30),
                        "PENDING",
                        new BigDecimal("79.99"),
                        List.of()
                )
        ));

        mockMvc.perform(get("/api/orders/all"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].userId").value(5))
            .andExpect(jsonPath("$[0].total").value(79.99));
    }
}
