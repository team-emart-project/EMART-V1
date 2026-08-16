package com.example.demo.controller;

import com.example.demo.dto.request.*;
import com.example.demo.dto.response.AuthResponse;
import com.example.demo.dto.response.UserResponse;
import com.example.demo.response.ApiResponse;
import com.example.demo.service.interfaces.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * Module 2 — Authentication. All endpoints are public.
 *
 * Access tokens only: there is no /refresh-token endpoint anywhere in this API.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    public AuthController(AuthService authService) {
        this.authService = authService;
    }


    private final AuthService authService;

    /** POST /api/auth/register -> 201 Created */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> register(
            @Valid @RequestBody RegisterRequest request) {

        UserResponse user = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(
                        "Registration successful. Your membership number has been emailed to you.",
                        user));
    }

    /** POST /api/auth/login -> JWT access token */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {

        return ResponseEntity.ok(ApiResponse.success("Login successful", authService.login(request)));
    }

    /**
     * Sign in with Google (OAuth 2.0 / OpenID Connect).
     *
     * The browser obtains an ID token from Google and posts it here; this
     * endpoint verifies it and returns OUR OWN JWT. From the next request
     * onwards a Google user is indistinguishable from a password user — same
     * token, same header, same authorization rules.
     *
     * Registers the user automatically on first sign-in, so there is no
     * separate "sign up with Google" endpoint to keep in step with this one.
     *
     * Public (permitAll) for the obvious reason: the caller has no token yet.
     */
    @PostMapping("/google")
    public ResponseEntity<ApiResponse<AuthResponse>> loginWithGoogle(
            @Valid @RequestBody GoogleLoginRequest request) {

        return ResponseEntity.ok(ApiResponse.success(
                "Signed in with Google", authService.loginWithGoogle(request)));
    }

    /**
     * POST /api/auth/logout
     *
     * With stateless JWT there is nothing to revoke on the server — the token
     * stays valid until it expires. The real logout is the frontend deleting
     * its copy. This endpoint exists so the client has something to call, and
     * it clears the SecurityContext for this request.
     *
     * (Server-side revocation would need a token blacklist table, which this
     * project deliberately does not have.)
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout() {
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok(ApiResponse.success(
                "Logged out. Please discard the access token on the client."));
    }

    /**
     * POST /api/auth/forgot-password
     *
     * Always returns 200, even for an unknown email — see AuthServiceImpl.
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {

        authService.forgotPassword(request);
        return ResponseEntity.ok(ApiResponse.success(
                "If that email is registered, a password reset link has been sent to it."));
    }

    /** POST /api/auth/reset-password */
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {

        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success(
                "Password has been reset. You can now log in with your new password."));
    }
}
