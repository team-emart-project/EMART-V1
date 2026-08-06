package com.example.demo.controller;

import com.example.demo.dto.request.*;
import com.example.demo.dto.response.ApiResponse;
import com.example.demo.dto.response.UserResponse;
import com.example.demo.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // GET /api/users/profile   (requires Authorization: Bearer <token>)
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserResponse>> getProfile(HttpServletRequest httpRequest) {
        Long userId = extractUserId(httpRequest);
        UserResponse profile = userService.getProfile(userId);
        return ResponseEntity.ok(ApiResponse.success("Profile fetched successfully", profile));
    }

    // PUT /api/users/profile   (requires Authorization: Bearer <token>)
    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            HttpServletRequest httpRequest,
            @Valid @RequestBody UpdateProfileRequest request) {
        Long userId = extractUserId(httpRequest);
        UserResponse updated = userService.updateProfile(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Profile updated successfully", updated));
    }

    // POST /api/users/change-password   (requires Authorization: Bearer <token>)
    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Object>> changePassword(
            HttpServletRequest httpRequest,
            @Valid @RequestBody ChangePasswordRequest request) {
        Long userId = extractUserId(httpRequest);
        userService.changePassword(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully"));
    }

    // POST /api/users/verify-email   (public)
    @PostMapping("/verify-email")
    public ResponseEntity<ApiResponse<Object>> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        userService.verifyEmail(request.getToken());
        return ResponseEntity.ok(ApiResponse.success("Email verified successfully"));
    }

    // POST /api/users/forgot-password   (public)
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Object>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        userService.forgotPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Password reset instructions sent to your email"));
    }

    // POST /api/users/reset-password   (public)
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Object>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        userService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Password reset successfully"));
    }

    // GET /api/users/dashboard   (requires Authorization: Bearer <token>)
    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboard(HttpServletRequest httpRequest) {
        Long userId = extractUserId(httpRequest);
        Map<String, Object> dashboard = userService.getDashboard(userId);
        return ResponseEntity.ok(ApiResponse.success("Dashboard fetched successfully", dashboard));
    }

    /**
     * userId is set as a request attribute by JwtAuthFilter after validating the token.
     */
    private Long extractUserId(HttpServletRequest request) {
        Object userId = request.getAttribute("userId");
        if (userId == null) {
            throw new com.example.demo.exception.UnauthorizedException("Invalid or missing authentication token");
        }
        return (Long) userId;
    }
}
