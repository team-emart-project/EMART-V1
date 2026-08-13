package com.example.demo.exception;

/**
 * Thrown when the request is well-formed but breaks a domain rule
 * (e.g. redeeming more points than a product allows). Mapped to HTTP 400.
 */
public class BusinessRuleViolationException extends RuntimeException {

    public BusinessRuleViolationException(String message) {
        super(message);
    }
}
