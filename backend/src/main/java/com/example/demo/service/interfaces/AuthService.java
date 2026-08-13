package com.example.demo.service.interfaces;

import com.example.demo.dto.request.*;
import com.example.demo.dto.response.AuthResponse;
import com.example.demo.dto.response.UserResponse;

/** Module 2 — registration and login. JWT access tokens only. */
public interface AuthService {

    UserResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    /**
     * Signs in with a Google ID token, registering the user on first use.
     *
     * Returns exactly the same {@link AuthResponse} as a password login, so the
     * front end and every downstream endpoint stay identical regardless of how
     * the user got here.
     */
    AuthResponse loginWithGoogle(GoogleLoginRequest request);

    /** Always succeeds from the caller's point of view (see impl for why). */
    void forgotPassword(ForgotPasswordRequest request);

    void resetPassword(ResetPasswordRequest request);
}
