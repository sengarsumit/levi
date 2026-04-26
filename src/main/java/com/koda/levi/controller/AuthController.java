package com.koda.levi.controller;

import com.koda.levi.dto.AuthResponse;
import com.koda.levi.dto.UserDto;
import com.koda.levi.service.Authservice;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final Authservice authService;

    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(@RequestBody UserDto userDto) {
        return ResponseEntity.ok(authService.signup(userDto));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody UserDto userDto) {
        return ResponseEntity.ok(authService.login(userDto));
    }
}
