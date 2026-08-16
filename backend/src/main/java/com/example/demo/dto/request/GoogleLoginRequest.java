package com.example.demo.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Body for POST /api/auth/google.
 *
 * ONE field, on purpose. The browser gets an ID token from Google and forwards
 * it here; everything about the user — email, name, picture, whether the email
 * is verified — is read from INSIDE that verified token, never from the request.
 *
 * If this DTO also carried an `email` field, a caller could send a real Google
 * token for their own account alongside somebody else's email address, and any
 * code that trusted the field would hand over the wrong account. Not having the
 * field makes that impossible rather than merely forbidden.
 */
public class GoogleLoginRequest {

    @NotBlank(message = "Google credential is required")
    private String credential;

    public GoogleLoginRequest() {
    }

    public GoogleLoginRequest(String credential) {
        this.credential = credential;
    }

    public String getCredential() {
        return credential;
    }

    public void setCredential(String credential) {
        this.credential = credential;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private String credential;

        public Builder credential(String credential) {
            this.credential = credential;
            return this;
        }

        public GoogleLoginRequest build() {
            return new GoogleLoginRequest(credential);
        }
    }
}
