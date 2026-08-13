package com.example.demo.controller;

import com.example.demo.dto.request.UpdateProfileRequest;
import com.example.demo.dto.response.UserResponse;
import com.example.demo.response.ApiResponse;
import com.example.demo.service.interfaces.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Module 3 — the logged-in user's profile.
 *
 * The path is /me rather than /{userId} so there is no id to tamper with.
 */
@RestController
@RequestMapping("/api/users")
public class UserController {
    public UserController(UserService userService) {
        this.userService = userService;
    }


    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser() {
        return ResponseEntity.ok(ApiResponse.success(
                "Profile retrieved successfully", userService.getCurrentUser()));
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> updateCurrentUser(
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Profile updated successfully", userService.updateCurrentUser(request)));
    }
}
