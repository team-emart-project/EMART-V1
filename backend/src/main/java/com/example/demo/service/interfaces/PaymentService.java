package com.example.demo.service.interfaces;

import com.example.demo.dto.request.PaymentVerifyRequest;
import com.example.demo.dto.response.PaymentResponse;

import java.util.List;

/** Module 9 — mock payment against a placed order. */
public interface PaymentService {

    PaymentResponse verifyPayment(Integer orderId, PaymentVerifyRequest request);

    /** Every attempt on the order, newest first. */
    List<PaymentResponse> getPaymentsForOrder(Integer orderId);
}
