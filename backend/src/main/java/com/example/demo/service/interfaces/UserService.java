package com.example.demo.service.interfaces;

import com.example.demo.dto.request.UpdateProfileRequest;
import com.example.demo.dto.response.UserResponse;

/** Module 3 — the logged-in user's own profile. */
public interface UserService {

    UserResponse getCurrentUser();

    UserResponse updateCurrentUser(UpdateProfileRequest request);
}
