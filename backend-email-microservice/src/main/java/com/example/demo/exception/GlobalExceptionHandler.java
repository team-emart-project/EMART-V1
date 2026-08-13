package com.example.demo.exception;

import com.example.demo.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Turns exceptions into the shared ErrorResponse envelope.
 *
 * Only failures that happen on the HTTP thread — i.e. while the payload is
 * being read and validated — can land here. Once the request has been accepted
 * the work moves to a background thread and any failure there is a log entry,
 * not a response.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * @Valid failures. The per-field map matters more here than anywhere else
     * in the project: the caller is another server, not a person, and
     * "items[0].quantity: Quantity must be at least 1" is the difference
     * between a five-minute fix and an afternoon of guessing.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex,
                                                          HttpServletRequest request) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(fe -> fieldErrors.putIfAbsent(fe.getField(), fe.getDefaultMessage()));

        log.warn("Rejected order email payload from {}: {}", request.getRemoteAddr(), fieldErrors);

        return ResponseEntity.badRequest()
                .body(ErrorResponse.validation("One or more fields are invalid",
                        request.getRequestURI(), fieldErrors));
    }

    /** Malformed JSON, or a value Jackson cannot coerce (a bad date, usually). */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException ex,
                                                          HttpServletRequest request) {
        log.warn("Unreadable request body from {}: {}", request.getRemoteAddr(), ex.getMessage());

        return ResponseEntity.badRequest()
                .body(ErrorResponse.of(400, "Bad Request",
                        "The request body could not be read as an order email payload",
                        request.getRequestURI()));
    }

    /**
     * Only reachable when a send is run inline (queue saturated, CallerRunsPolicy).
     * 502 rather than 500: this service is fine, the mail server is not.
     */
    @ExceptionHandler(EmailDeliveryException.class)
    public ResponseEntity<ErrorResponse> handleDelivery(EmailDeliveryException ex,
                                                        HttpServletRequest request) {
        log.error("Delivery failed for orderNo={}", ex.getOrderNo(), ex);

        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(ErrorResponse.of(502, "Bad Gateway",
                        "The email could not be handed to the mail server",
                        request.getRequestURI()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unexpected failure handling {} {}", request.getMethod(), request.getRequestURI(), ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of(500, "Internal Server Error",
                        "Something went wrong in the email service",
                        request.getRequestURI()));
    }
}
