package com.example.demo.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.demo.dto.PaymentRequestDTO;
import com.example.demo.dto.PaymentResponseDTO;
import com.example.demo.service.PaymentService;

@RestController
@RequestMapping("/api/payments")
@CrossOrigin(origins = "*")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    // Make Payment
    @PostMapping
    public ResponseEntity<PaymentResponseDTO> makePayment(
            @RequestBody PaymentRequestDTO request) {

        PaymentResponseDTO response = paymentService.makePayment(request);

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    // Get Payment By ID
    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentResponseDTO> getPaymentById(
            @PathVariable Integer paymentId) {

        return ResponseEntity.ok(
                paymentService.getPaymentById(paymentId));
    }

    // Get All Payments
    @GetMapping
    public ResponseEntity<List<PaymentResponseDTO>> getAllPayments() {

        return ResponseEntity.ok(
                paymentService.getAllPayments());
    }

    // Get Payments By Order ID
    @GetMapping("/order/{orderId}")
    public ResponseEntity<List<PaymentResponseDTO>> getPaymentsByOrderId(
            @PathVariable Integer orderId) {

        return ResponseEntity.ok(
                paymentService.getPaymentsByOrderId(orderId));
    }

}