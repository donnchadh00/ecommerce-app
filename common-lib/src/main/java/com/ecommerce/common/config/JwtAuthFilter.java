package com.ecommerce.common.config;

import com.ecommerce.common.security.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String jwt = authHeader.substring(7);

        // If you have validateToken, prefer to short-circuit on invalid tokens
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            if (!jwtService.validateToken(jwt)) {
                filterChain.doFilter(request, response);
                return;
            }

            final String userEmail = jwtService.extractEmail(jwt);
            if (userEmail == null || userEmail.isBlank()) {
                filterChain.doFilter(request, response);
                return;
            }

            // Prefer a roles list if available; fallback to single role
            Collection<GrantedAuthority> authorities = new ArrayList<>();
            List<String> roles = null;
            try {
                roles = jwtService.extractRoles(jwt);
            } catch (Throwable ignored) {}

            if (roles != null && !roles.isEmpty()) {
                for (String r : roles) {
                    String norm = normalizeRole(r);
                    if (norm != null) authorities.add(new SimpleGrantedAuthority(norm));
                }
            } else {
                // fallback to legacy single role
                String role = jwtService.extractRole(jwt);
                String norm = normalizeRole(role);
                if (norm != null) authorities.add(new SimpleGrantedAuthority(norm));
            }

            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(userEmail, null, authorities);
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authToken);
        } catch (Exception ex) {
            // Don’t authenticate on parsing errors; just continue so Spring returns 401/403 as appropriate
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    private String normalizeRole(String raw) {
        if (raw == null) return null;
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) return null;
        // normalize case and prefix
        String upper = trimmed.toUpperCase();
        return upper.startsWith("ROLE_") ? upper : "ROLE_" + upper;
    }
}
