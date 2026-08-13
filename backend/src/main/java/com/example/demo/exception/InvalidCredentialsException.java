package com.example.demo.exception;

/** Wrong email or password. Mapped to HTTP 401. */
public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException(String message) {
        super(message);
    }
}
