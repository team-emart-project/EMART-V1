package com.example.exception;

import com.example.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * One place that turns exceptions into HTTP responses.
 *
 * @RestControllerAdvice makes these handlers apply to every controller, so no
 * controller ever needs a try/catch — they just throw and this translates.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex,
                                                        HttpServletRequest request) {
        log.warn("Resource not found: {}", ex.getMessage());
        return build(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage(), request);
    }

    @ExceptionHandler(BusinessRuleViolationException.class)
    public ResponseEntity<ErrorResponse> handleBusinessRule(BusinessRuleViolationException ex,
                                                             HttpServletRequest request) {
        log.warn("Business rule violated: {}", ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage(), request);
    }

    @ExceptionHandler(UnauthorizedActionException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized(UnauthorizedActionException ex,
                                                             HttpServletRequest request) {
        log.warn("Unauthorized action: {}", ex.getMessage());
        return build(HttpStatus.FORBIDDEN, "Forbidden", ex.getMessage(), request);
    }

    /** Triggered by @Valid failures on request DTOs. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex,
                                                           HttpServletRequest request) {
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(fe -> fieldErrors.put(fe.getField(), fe.getDefaultMessage()));

        ErrorResponse body = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Validation Failed")
                .message("One or more fields are invalid")
                .path(request.getRequestURI())
                .fieldErrors(fieldErrors)
                .build();

        return ResponseEntity.badRequest().body(body);
    }

    /** Catch-all so an unexpected bug never leaks a stack trace to the client. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex,
                                                           HttpServletRequest request) {
        log.error("Unexpected error", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
                "Something went wrong. Please try again later.", request);
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String error,
                                                 String message, HttpServletRequest request) {
        return ResponseEntity.status(status).body(
                ErrorResponse.builder()
                        .status(status.value())
                        .error(error)
                        .message(message)
                        .path(request.getRequestURI())
                        .build());
    }
}
