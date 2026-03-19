package com.ecommerce.auth_service;

import com.ecommerce.auth_service.config.SecurityConfig;
import com.ecommerce.auth_service.controller.AuthController;
import com.ecommerce.auth_service.model.User;
import com.ecommerce.auth_service.repository.UserRepository;
import com.ecommerce.common.config.JwtAuthFilter;
import com.ecommerce.common.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(SecurityConfig.class)
class AuthControllerWebMvcTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private JwtAuthFilter jwtAuthFilter;

    @Test
    void registerCreatesDefaultUserRole() throws Exception {
        when(userRepository.findByEmail("new@demo.local")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("Password123!")).thenReturn("encoded-password");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "email": "new@demo.local",
                      "password": "Password123!",
                      "role": "ADMIN"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(content().string("User registered successfully"));

        verify(userRepository).save(any(User.class));
    }

    @Test
    void loginReturnsJwtWhenCredentialsMatch() throws Exception {
        User user = new User();
        user.setId(42L);
        user.setEmail("user@demo.local");
        user.setPassword("encoded-password");
        user.setRole("USER");

        when(userRepository.findByEmail("user@demo.local")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Password123!", "encoded-password")).thenReturn(true);
        when(jwtService.generateToken("user@demo.local", "USER", 42L)).thenReturn("signed-jwt");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "email": "user@demo.local",
                      "password": "Password123!"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").value("signed-jwt"));
    }

    @Test
    void loginRejectsInvalidCredentials() throws Exception {
        User user = new User();
        user.setEmail("user@demo.local");
        user.setPassword("encoded-password");

        when(userRepository.findByEmail("user@demo.local")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "encoded-password")).thenReturn(false);

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "email": "user@demo.local",
                      "password": "wrong-password"
                    }
                    """))
            .andExpect(status().isUnauthorized())
            .andExpect(content().string("Invalid credentials"));

        verify(jwtService, never()).generateToken(any(), any(), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createUserAllowsAdminToChooseRole() throws Exception {
        when(userRepository.findByEmail("staff@demo.local")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("Password123!")).thenReturn("encoded-password");

        mockMvc.perform(post("/api/auth/create-user")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "email": "staff@demo.local",
                      "password": "Password123!",
                      "role": "ADMIN"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(content().string("User created successfully"));

        verify(userRepository).save(any(User.class));
    }

}
