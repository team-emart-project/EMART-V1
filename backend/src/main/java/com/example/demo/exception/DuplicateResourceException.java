package com.example.demo.exception;

/**
 * Thrown when creating something that already exists — a second account on one
 * email, a second e-MART card for one user, a duplicate wishlist entry.
 *
 * Mapped to HTTP 409 Conflict: the request is valid, but it clashes with the
 * current state of the server. (400 would wrongly imply the request was
 * malformed.)
 */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }

    public DuplicateResourceException(String resource, String field, Object value) {
        super("%s already exists with %s: %s".formatted(resource, field, value));
    }
}
