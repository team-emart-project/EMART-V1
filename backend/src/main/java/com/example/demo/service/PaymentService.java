package com.example.demo.service;

import java.util.List;

import com.example.demo.dto.PaymentRequestDTO;
import com.example.demo.dto.PaymentResponseDTO;

public interface PaymentService {

    // Make a payment
    PaymentResponseDTO makePayment(PaymentRequestDTO request);

    // Get payment by Payment ID
    PaymentResponseDTO getPaymentById(Integer paymentId);

    // Get all payments
    List<PaymentResponseDTO> getAllPayments();

    // Get payment by Order ID
    List<PaymentResponseDTO> getPaymentsByOrderId(Integer orderId);

}