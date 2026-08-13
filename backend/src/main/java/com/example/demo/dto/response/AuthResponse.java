package com.example.demo.dto.response;
/**
 * Returned by login.
 *
 * There is deliberately no refreshToken field — this project uses access
 * tokens only. When the token expires the user logs in again.
 */
public class AuthResponse {

    private String accessToken;
    private String tokenType = "Bearer";

    /** Lifetime in milliseconds, so the frontend knows when to re-authenticate. */
    private Long expiresInMs;

    private UserResponse user;

    public AuthResponse() {
    }

    public AuthResponse(String accessToken, String tokenType, Long expiresInMs, UserResponse user) {
        this.accessToken = accessToken;
        this.tokenType = tokenType;
        this.expiresInMs = expiresInMs;
        this.user = user;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public Long getExpiresInMs() {
        return expiresInMs;
    }

    public void setExpiresInMs(Long expiresInMs) {
        this.expiresInMs = expiresInMs;
    }

    public UserResponse getUser() {
        return user;
    }

    public void setUser(UserResponse user) {
        this.user = user;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private String accessToken;
        private String tokenType = "Bearer";
        private Long expiresInMs;
        private UserResponse user;

        public Builder accessToken(String accessToken) {
            this.accessToken = accessToken;
            return this;
        }

        public Builder tokenType(String tokenType) {
            this.tokenType = tokenType;
            return this;
        }

        public Builder expiresInMs(Long expiresInMs) {
            this.expiresInMs = expiresInMs;
            return this;
        }

        public Builder user(UserResponse user) {
            this.user = user;
            return this;
        }

        public AuthResponse build() {
            return new AuthResponse(accessToken, tokenType, expiresInMs, user);
        }

    }
}
