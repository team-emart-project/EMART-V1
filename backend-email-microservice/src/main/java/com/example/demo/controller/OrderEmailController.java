package com.example.demo.controller;

import com.example.demo.dto.request.OrderEmailRequest;
import com.example.demo.dto.response.EmailSendResponse;
import com.example.demo.response.ApiResponse;
import com.example.demo.service.interfaces.OrderEmailService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The endpoint both backends call.
 *
 * There is exactly one, and it takes a finished order. This service has no
 * /orders, no /invoices and no database: it cannot look an order up, which is
 * precisely what keeps it decoupled from the two backends and from the shared
 * MySQL schema.
 */
@RestController
@RequestMapping("/api")
public class OrderEmailController {

    private final OrderEmailService orderEmailService;

    public OrderEmailController(OrderEmailService orderEmailService) {
        this.orderEmailService = orderEmailService;
    }

    /**
     * POST /api/send-order-email
     *
     * 202 ACCEPTED, not 200 OK — the honest status code for "I have taken
     * responsibility for this, I have not done it yet". The caller should
     * treat a 202 as success and move on; it must never block a customer's
     * checkout waiting for an inbox.
     */
    @PostMapping("/send-order-email")
    public ResponseEntity<ApiResponse<EmailSendResponse>> sendOrderEmail(
            @Valid @RequestBody OrderEmailRequest request) {

        EmailSendResponse result = orderEmailService.acceptOrderPlaced(request);

        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success("Order email accepted for delivery", result));
    }
}
