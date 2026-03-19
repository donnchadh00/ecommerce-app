package com.ecommerce.common.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.WeakKeyException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.*;
import java.util.function.Function;

@Service
public class JwtService {

    private final Key signingKey;
    private final long expirationTimeMs;

    public JwtService(
            @Value("${JWT_SECRET}") String secretKey,
            @Value("${JWT_EXPIRATION_MS:36000000}") long expirationTimeMs) {
        this.signingKey = buildSigningKey(secretKey);
        this.expirationTimeMs = expirationTimeMs;
    }

    private Key getSigningKey() {
        return signingKey;
    }

    /* ===================== Token generation ===================== */

    public String generateToken(String email, String role, Long userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role);
        claims.put("roles", List.of(role));
        claims.put("userId", userId);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(email)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationTimeMs))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /* ===================== Validation ===================== */

    public boolean validateToken(String token) {
        try {
            Claims claims = parseToken(token);
            return claims.getExpiration() == null || claims.getExpiration().after(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /* ===================== Extractors ===================== */

    public String extractUsername(String token) { return extractClaim(token, Claims::getSubject); }

    public String extractEmail(String token) { return extractClaim(token, Claims::getSubject); }

    public String extractRole(String token) {
        return extractClaim(token, c -> c.get("role", String.class));
    }

    public List<String> extractRoles(String token) {
        return extractClaim(token, claims -> {
            // Preferred: array claim "roles"
            Object raw = claims.get("roles");
            if (raw instanceof Collection<?> col) {
                List<String> roles = new ArrayList<>();
                for (Object o : col) {
                    if (o != null) roles.add(String.valueOf(o));
                }
                return normalizeRoles(roles);
            }
            // Fallback: single "role"
            String single = claims.get("role", String.class);
            if (single != null) return normalizeRoles(List.of(single));
            // Nothing present
            return Collections.emptyList();
        });
    }

    public Long extractUserId(String token) {
        // JJWT returns numbers as Integer/Long depending on size; handle both
        return extractClaim(token, claims -> {
            Object v = claims.get("userId");
            if (v instanceof Number n) return n.longValue();
            if (v instanceof String s) {
                try { return Long.parseLong(s); } catch (NumberFormatException ignored) {}
            }
            return null;
        });
    }

    /* ===================== Internals ===================== */

    private <T> T extractClaim(String token, Function<Claims, T> resolver) {
        Claims claims = parseToken(token);
        return resolver.apply(claims);
    }

    private Claims parseToken(String token) {
        String t = cleanToken(token);
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(t)
                .getBody();
    }

    private String cleanToken(String token) {
        return token == null ? "" : token.replace("Bearer ", "").trim();
    }

    private Key buildSigningKey(String secretKey) {
        String normalized = secretKey == null ? "" : secretKey.trim();
        if (normalized.isEmpty()) {
            throw new IllegalStateException("JWT_SECRET must be configured");
        }

        try {
            return Keys.hmacShaKeyFor(normalized.getBytes(StandardCharsets.UTF_8));
        } catch (WeakKeyException ex) {
            throw new IllegalStateException("JWT_SECRET must be at least 32 bytes for HS256", ex);
        }
    }

    private List<String> normalizeRoles(List<String> roles) {
        List<String> out = new ArrayList<>();
        for (String r : roles) {
            if (r == null) continue;
            String trimmed = r.trim();
            if (trimmed.isEmpty()) continue;
            String upper = trimmed.toUpperCase();
            out.add(upper.startsWith("ROLE_") ? upper : "ROLE_" + upper);
        }
        return out;
    }
}
