package com.example.demo.controller;

import com.example.demo.dto.request.PaymentVerifyRequest;
import com.example.demo.dto.response.PaymentResponse;
import com.example.demo.response.ApiResponse;
import com.example.demo.service.interfaces.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Module 9 — Payment. All endpoints require a logged-in user. */
@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /**
     * POST /api/payments/{orderId}/verify
     *
     * Mock gateway: a card number ending in 0 is DECLINED, anything else is
     * approved. That makes the failure path testable without a real gateway.
     */
    @PostMapping("/{orderId}/verify")
    public ResponseEntity<ApiResponse<PaymentResponse>> verifyPayment(
            @PathVariable Integer orderId,
            @Valid @RequestBody PaymentVerifyRequest request) {

        PaymentResponse response = paymentService.verifyPayment(orderId, request);
        String message = "SUCCESS".equals(response.getStatus())
                ? "Payment successful. Your invoice is ready to download."
                : "Payment was declined. Please try a different card.";

        return ResponseEntity.ok(ApiResponse.success(message, response));
    }

    /** GET /api/payments/{orderId} — every attempt on this order, newest first. */
    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> getPayments(
            @PathVariable Integer orderId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Payments retrieved successfully", paymentService.getPaymentsForOrder(orderId)));
    }
}
