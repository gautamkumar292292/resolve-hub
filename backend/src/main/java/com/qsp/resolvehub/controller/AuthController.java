package com.qsp.resolvehub.controller;


import com.qsp.resolvehub.dto.request.LoginRequest;
import com.qsp.resolvehub.dto.request.RegisterRequest;
import com.qsp.resolvehub.dto.response.AuthResponse;
import com.qsp.resolvehub.dto.response.RegisterResponse;
import com.qsp.resolvehub.dto.response.UserResponse;
import com.qsp.resolvehub.service.AuthService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(
            @Valid @RequestBody RegisterRequest request) {

        RegisterResponse response = authService.register(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request) {

        AuthResponse response = authService.login(request);

        return ResponseEntity.ok(response);
    }


    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(
            Authentication authentication) {

        UserResponse response =
                authService.getCurrentUser(authentication.getName());

        return ResponseEntity.ok(response);
    }


    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            Authentication authentication) {

        authService.logout(authentication.getName());

        return ResponseEntity.noContent().build();
    }
}