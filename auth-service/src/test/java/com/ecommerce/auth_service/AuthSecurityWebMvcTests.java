package com.ecommerce.auth_service;

import com.ecommerce.auth_service.config.SecurityConfig;
import com.ecommerce.auth_service.controller.AuthController;
import com.ecommerce.auth_service.repository.UserRepository;
import com.ecommerce.common.config.JwtAuthFilter;
import com.ecommerce.common.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class})
class AuthSecurityWebMvcTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private JwtService jwtService;

    @Test
    void registerRemainsPublicWithoutAuthentication() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "email": "not-an-email",
                      "password": "short"
                    }
                    """))
            .andExpect(status().isBadRequest());
    }

    @Test
    void meRejectsAnonymousRequests() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
            .andExpect(status().isForbidden());
    }

    @Test
    void meAllowsAuthenticatedJwtRequests() throws Exception {
        when(jwtService.validateToken("valid-token")).thenReturn(true);
        when(jwtService.extractEmail("valid-token")).thenReturn("admin@demo.local");
        when(jwtService.extractRoles("valid-token")).thenReturn(List.of("ROLE_ADMIN"));

        mockMvc.perform(get("/api/auth/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token"))
            .andExpect(status().isOk())
            .andExpect(content().string("Authenticated as: admin@demo.local"));
    }

    @Test
    void adminOnlyRejectsNonAdminJwtRequests() throws Exception {
        when(jwtService.validateToken("user-token")).thenReturn(true);
        when(jwtService.extractEmail("user-token")).thenReturn("user@demo.local");
        when(jwtService.extractRoles("user-token")).thenReturn(List.of("ROLE_USER"));

        mockMvc.perform(get("/api/auth/admin/only")
                .header(HttpHeaders.AUTHORIZATION, "Bearer user-token"))
            .andExpect(status().isForbidden());
    }
}
