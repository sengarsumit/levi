package com.koda.levi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {
    /**
     * Backward-compatible alias for clients that currently read "token".
     */
    private String token;

    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private long expiresInMs;

    public AuthResponse(String token) {
        this.token = token;
        this.accessToken = token;
        this.tokenType = "Bearer";
    }

    public static AuthResponse bearer(String accessToken, String refreshToken, long expiresInMs) {
        return AuthResponse.builder()
                .token(accessToken)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresInMs(expiresInMs)
                .build();
    }
}
