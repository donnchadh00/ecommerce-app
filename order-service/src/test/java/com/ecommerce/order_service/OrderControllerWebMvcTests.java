package com.ecommerce.order_service;

import com.ecommerce.common.config.JwtAuthFilter;
import com.ecommerce.common.security.JwtService;
import com.ecommerce.order_service.config.SecurityConfig;
import com.ecommerce.order_service.controller.OrderController;
import com.ecommerce.order_service.dto.OrderCreateRequest;
import com.ecommerce.order_service.dto.OrderResponse;
import com.ecommerce.order_service.dto.TraceTimelineResponse;
import com.ecommerce.order_service.dto.TraceTimelineSpanResponse;
import com.ecommerce.order_service.service.CurrentTraceContext;
import com.ecommerce.order_service.service.OrderService;
import com.ecommerce.order_service.service.TempoTraceService;
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
    private CurrentTraceContext currentTraceContext;

    @MockitoBean
    private TempoTraceService tempoTraceService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private JwtAuthFilter jwtAuthFilter;

    @Test
    @WithMockUser(roles = "USER")
    void placeOrderAcceptsValidatedRequestDto() throws Exception {
        String traceId = "3f7ad51f4f9446fe885fd5c15cb0d8a2";
        when(currentTraceContext.currentTraceId()).thenReturn(java.util.Optional.empty());
        when(orderService.createOrder(any(OrderCreateRequest.class), any(), eq(traceId))).thenReturn(
                new OrderResponse(
                        101L,
                        42L,
                        LocalDateTime.of(2026, 3, 19, 13, 0),
                        "PENDING",
                        traceId,
                        new BigDecimal("129.99"),
                        List.of(new OrderResponse.OrderItemResponse(1L, 7L, 2, new BigDecimal("65.00")))
                )
        );

        mockMvc.perform(post("/api/orders")
                .header("X-Trace-Id", traceId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "items": [
                        { "productId": 7, "quantity": 2 }
                      ]
                    }
                    """))
            .andExpect(status().isAccepted())
            .andExpect(header().string("X-Trace-Id", traceId))
            .andExpect(header().string("Location", "/api/orders/101"))
            .andExpect(jsonPath("$.orderId").value(101))
            .andExpect(jsonPath("$.status").value("PENDING"))
            .andExpect(jsonPath("$.traceId").value(traceId));
    }

    @Test
    @WithMockUser(roles = "USER")
    void placeOrderRejectsInvalidItems() throws Exception {
        when(currentTraceContext.currentTraceId()).thenReturn(java.util.Optional.empty());
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
    @WithMockUser(roles = "USER")
    void placeOrderUsesActiveOtelTraceIdWhenPresent() throws Exception {
        String otelTraceId = "3f7ad51f4f9446fe885fd5c15cb0d8a2";
        when(currentTraceContext.currentTraceId()).thenReturn(java.util.Optional.of(otelTraceId));
        when(orderService.createOrder(any(OrderCreateRequest.class), any(), eq(otelTraceId))).thenReturn(
                new OrderResponse(
                        202L,
                        42L,
                        LocalDateTime.of(2026, 3, 20, 10, 30),
                        "PENDING",
                        otelTraceId,
                        new BigDecimal("32.50"),
                        List.of(new OrderResponse.OrderItemResponse(2L, 8L, 1, new BigDecimal("32.50")))
                )
        );

        mockMvc.perform(post("/api/orders")
                .header("X-Trace-Id", "not-a-tempo-trace-id")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "items": [
                        { "productId": 8, "quantity": 1 }
                      ]
                    }
                    """))
            .andExpect(status().isAccepted())
            .andExpect(header().string("X-Trace-Id", otelTraceId))
            .andExpect(jsonPath("$.traceId").value(otelTraceId));
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
                        "trace-order-11",
                        new BigDecimal("49.99"),
                        List.of(new OrderResponse.OrderItemResponse(5L, 3L, 1, new BigDecimal("49.99")))
                )
        ));

        mockMvc.perform(get("/api/orders"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(11))
            .andExpect(jsonPath("$[0].traceId").value("trace-order-11"))
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
                        "trace-order-21",
                        new BigDecimal("79.99"),
                        List.of()
                )
        ));

        mockMvc.perform(get("/api/orders/all"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].userId").value(5))
            .andExpect(jsonPath("$[0].traceId").value("trace-order-21"))
            .andExpect(jsonPath("$[0].total").value(79.99));
    }

    @Test
    @WithMockUser(roles = "USER")
    void getTraceTimelineReturnsNormalizedTraceData() throws Exception {
        when(tempoTraceService.fetchTimeline("trace-123")).thenReturn(
            new TraceTimelineResponse(
                "trace-123",
                true,
                null,
                List.of(
                    new TraceTimelineSpanResponse("order-service", "POST /api/orders", 0, 52, "STATUS_CODE_OK")
                )
            )
        );

        mockMvc.perform(get("/api/orders/traces/trace-123"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.traceId").value("trace-123"))
            .andExpect(jsonPath("$.available").value(true))
            .andExpect(jsonPath("$.spans[0].service").value("order-service"))
            .andExpect(jsonPath("$.spans[0].durationMs").value(52));
    }
}
