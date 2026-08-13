package com.example.demo.exception;

/** Bad or expired reset-password token / JWT. Mapped to HTTP 400. */
public class InvalidTokenException extends RuntimeException {
    public InvalidTokenException(String message) {
        super(message);
    }
}
