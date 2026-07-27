package com.example.demo.controller;

import com.example.demo.dto.request.LoginRequest;
import com.example.demo.dto.request.RegisterRequest;
import com.example.demo.dto.response.ApiResponse;
import com.example.demo.dto.response.AuthResponse;
import com.example.demo.dto.response.UserResponse;
import com.example.demo.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // POST /api/auth/register
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> register(@Valid @RequestBody RegisterRequest request) {
        UserResponse user = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Registration successful. Please verify your email.", user));
    }

    // POST /api/auth/login
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse authResponse = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", authResponse));
    }

    // POST /api/auth/logout
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Object>> logout(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        String token = (authHeader != null && authHeader.startsWith("Bearer "))
                ? authHeader.substring(7) : null;
        authService.logout(token);
        return ResponseEntity.ok(ApiResponse.success("Logout successful"));
    }
}
