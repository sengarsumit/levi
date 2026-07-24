package com.koda.levi.config;

import com.koda.levi.entity.Users;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Date;
import java.util.Map;
import java.util.Optional;

@Service
public class JwtService {

    // Injecting from application.yml
    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.expiration-ms}")
    private long expiration;

    /**
     * Converts the String secret into a SecretKey object.
     * Note: The secret must be at least 32 characters (256 bits) for HS256.
     */
    private SecretKey getSignKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public long getAccessExpirationMs() {
        return expiration;
    }

    public String generateToken(Users user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId().toString());
        claims.put("systemId", user.getSystemId());
        claims.put("role", user.getRole());

        return generateToken(user.getEmail(), claims);
    }

    public String generateToken(String email) {
        return generateToken(email, Map.of());
    }

    private String generateToken(String email, Map<String, Object> claims) {
        return Jwts.builder()
                .claims(claims)
                .subject(email)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSignKey()) // JJWT automatically detects the algorithm based on key size
                .compact();
    }

    public String extractEmail(String token) {
        return extractAllClaims(token).getSubject();
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSignKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Optional<Long> extractTenantId(String token) {
        try {
            Claims claims = extractAllClaims(token);
            Object tenantId = claims.get("tenantId");
            if (tenantId == null) {
                tenantId = claims.get("organizationId");
            }

            if (tenantId == null) {
                return Optional.empty();
            }

            if (tenantId instanceof Number number) {
                return Optional.of(number.longValue());
            }

            return Optional.of(Long.parseLong(tenantId.toString()));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public boolean isValid(String token) {
        try {
            // If this doesn't throw an exception (ExpiredJwtException, MalformedJwtException, etc.), it's valid
            extractEmail(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            // Logging the error is usually helpful for debugging
            return false;
        }
    }

    public boolean isValid(String token, UserDetails userDetails) {
        try {
            String email = extractEmail(token);
            return email.equals(userDetails.getUsername()) && !isExpired(token);
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private boolean isExpired(String token) {
        return extractAllClaims(token).getExpiration().before(new Date());
    }
}
