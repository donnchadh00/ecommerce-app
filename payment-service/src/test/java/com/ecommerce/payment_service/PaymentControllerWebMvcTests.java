package com.ecommerce.payment_service;

import com.ecommerce.common.config.JwtAuthFilter;
import com.ecommerce.common.security.JwtService;
import com.ecommerce.payment_service.config.SecurityConfig;
import com.ecommerce.payment_service.controller.PaymentController;
import com.ecommerce.payment_service.dto.PaymentRequestDto;
import com.ecommerce.payment_service.dto.PaymentResponseDto;
import com.ecommerce.payment_service.model.PaymentStatus;
import com.ecommerce.payment_service.service.PaymentService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(SecurityConfig.class)
class PaymentControllerWebMvcTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentService paymentService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private JwtAuthFilter jwtAuthFilter;

    @Test
    @WithMockUser(roles = "USER")
    void createPaymentAcceptsValidatedRequestDto() throws Exception {
        when(paymentService.initiatePayment(any(PaymentRequestDto.class))).thenReturn(paymentResponse(41L, PaymentStatus.AUTHORIZED));

        mockMvc.perform(post("/api/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "orderId": 101,
                      "userId": 42,
                      "amount": 129.99,
                      "currency": "EUR"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(41))
            .andExpect(jsonPath("$.orderId").value(101))
            .andExpect(jsonPath("$.status").value("AUTHORIZED"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void createPaymentRejectsInvalidPayload() throws Exception {
        mockMvc.perform(post("/api/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "orderId": null,
                      "userId": 42,
                      "amount": 0,
                      "currency": "EURO"
                    }
                    """))
            .andExpect(status().isBadRequest());

        verify(paymentService, never()).initiatePayment(any(PaymentRequestDto.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getPaymentReturnsPaymentDto() throws Exception {
        when(paymentService.getPaymentById(41L)).thenReturn(paymentResponse(41L, PaymentStatus.SUCCESSFUL));

        mockMvc.perform(get("/api/payments/41"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.provider").value("stripe"))
            .andExpect(jsonPath("$.providerPaymentId").value("pi_demo_41"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void refundReturnsUpdatedPaymentDto() throws Exception {
        when(paymentService.refundPayment(41L)).thenReturn(paymentResponse(41L, PaymentStatus.REFUNDED));

        mockMvc.perform(post("/api/payments/41/refund"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("REFUNDED"));
    }

    private PaymentResponseDto paymentResponse(Long id, PaymentStatus status) {
        return PaymentResponseDto.builder()
                .id(id)
                .orderId(101L)
                .userId(42L)
                .amount(new BigDecimal("129.99"))
                .currency("EUR")
                .provider("stripe")
                .providerPaymentId("pi_demo_41")
                .status(status)
                .createdAt(LocalDateTime.of(2026, 3, 19, 13, 45))
                .updatedAt(LocalDateTime.of(2026, 3, 19, 13, 46))
                .build();
    }
}
