package com.example.demo.dto.request;

import com.example.demo.validation.annotation.ValidPassword;
import jakarta.validation.constraints.NotBlank;
public class ResetPasswordRequest {

    @NotBlank(message = "Reset token is required")
    private String token;

    @ValidPassword
    private String newPassword;

    public ResetPasswordRequest() {
    }

    public ResetPasswordRequest(String token, String newPassword) {
        this.token = token;
        this.newPassword = newPassword;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private String token;
        private String newPassword;

        public Builder token(String token) {
            this.token = token;
            return this;
        }

        public Builder newPassword(String newPassword) {
            this.newPassword = newPassword;
            return this;
        }

        public ResetPasswordRequest build() {
            return new ResetPasswordRequest(token, newPassword);
        }

    }
}
