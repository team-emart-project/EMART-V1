package com.example.demo.exception;

/**
 * Thrown when a row exists but belongs to somebody else.
 * Mapped to HTTP 403 — deliberately distinct from 404 so we do not leak
 * whether the id exists.
 */
public class UnauthorizedActionException extends RuntimeException {

    public UnauthorizedActionException(String message) {
        super(message);
    }
}
