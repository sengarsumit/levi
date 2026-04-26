package com.koda.levi.service;

import com.koda.levi.config.JwtService;
import com.koda.levi.dto.AuthResponse;
import com.koda.levi.dto.UserDto;
import com.koda.levi.entity.Users;
import com.koda.levi.exception.BadRequestException;
import com.koda.levi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class Authservice {

    private final UserRepository repo;
    private final PasswordEncoder encoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthResponse signup(UserDto userDto) {
        if (repo.findByEmail(userDto.getEmail()).isPresent()) {
            throw new BadRequestException("Email already exists");
        }

        Users user = Users.builder()
                .name(userDto.getName())
                .email(userDto.getEmail())
                .password(encoder.encode(userDto.getPassword()))
                .role("ROLE_USER")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        repo.save(user);

        String token = jwtService.generateToken(user.getEmail());
        return new AuthResponse(token);
    }

    public AuthResponse login(UserDto userDto) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        userDto.getEmail(),
                        userDto.getPassword()
                )
        );

        String token = jwtService.generateToken(userDto.getEmail());
        return new AuthResponse(token);
    }
}
