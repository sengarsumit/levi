package com.koda.levi.service;

import com.koda.levi.config.JwtService;
import com.koda.levi.dto.AuthResponse;
import com.koda.levi.dto.LoginRequest;
import com.koda.levi.dto.RefreshTokenRequest;
import com.koda.levi.dto.SignupRequest;
import com.koda.levi.entity.RefreshToken;
import com.koda.levi.entity.Users;
import com.koda.levi.exception.BadRequestException;
import com.koda.levi.exception.ResourceNotFoundException;
import com.koda.levi.exception.UnauthorizedException;
import com.koda.levi.repository.RefreshTokenRepository;
import com.koda.levi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class Authservice {

    private final UserRepository repo;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder encoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Value("${app.jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    @Transactional
    public AuthResponse signup(SignupRequest request) {
        String email = normalizeEmail(request.getEmail());
        if (repo.findByEmail(email).isPresent()) {
            throw new BadRequestException("Email already exists");
        }

        Users user = Users.builder()
                .systemId(generateUserSystemId())
                .name(request.getName().trim())
                .email(email)
                .password(encoder.encode(request.getPassword()))
                .role("ROLE_USER")
                .build();

        Users savedUser = repo.save(user);
        return buildAuthResponse(savedUser);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request.getEmail());
        Users user = repo.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        authenticate(email, request.getPassword());
        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        if (refreshToken.isRevoked() || refreshToken.isExpired()) {
            refreshToken.setRevoked(true);
            refreshTokenRepository.save(refreshToken);
            throw new UnauthorizedException("Refresh token expired or revoked");
        }

        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);

        return buildAuthResponse(refreshToken.getUser());
    }

    @Transactional
    public void logout(RefreshTokenRequest request) {
        refreshTokenRepository.findByToken(request.getRefreshToken())
                .ifPresent(refreshToken -> {
                    refreshToken.setRevoked(true);
                    refreshTokenRepository.save(refreshToken);
                });
    }

    private void authenticate(String email, String password) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            email,
                            password
                    )
            );
        } catch (BadCredentialsException e) {
            throw new BadRequestException("Invalid password");
        }
    }

    private AuthResponse buildAuthResponse(Users user) {
        String accessToken = jwtService.generateToken(user);
        RefreshToken refreshToken = createRefreshToken(user);
        return AuthResponse.bearer(accessToken, refreshToken.getToken(), jwtService.getAccessExpirationMs());
    }

    private RefreshToken createRefreshToken(Users user) {
        RefreshToken refreshToken = RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .user(user)
                .expiresAt(Instant.now().plusMillis(refreshExpirationMs))
                .revoked(false)
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    private String generateUserSystemId() {
        long nextNumber = Math.max(1000, repo.count() + 1000);
        String systemId = "USR-" + nextNumber;

        while (repo.existsBySystemId(systemId)) {
            nextNumber++;
            systemId = "USR-" + nextNumber;
        }

        return systemId;
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
