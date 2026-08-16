package com.example.demo.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * The failure envelope, matching the two backends field for field — including
 * `fieldErrors`, which is how a caller finds out exactly which part of the
 * order payload it got wrong.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(boolean success,
                            int status,
                            String error,
                            String message,
                            String path,
                            Map<String, String> fieldErrors,
                            LocalDateTime timestamp) {

    public static ErrorResponse of(int status, String error, String message, String path) {
        return new ErrorResponse(false, status, error, message, path, null, LocalDateTime.now());
    }

    public static ErrorResponse validation(String message, String path, Map<String, String> fieldErrors) {
        return new ErrorResponse(false, 400, "Validation Failed", message, path, fieldErrors, LocalDateTime.now());
    }
}
