package com.example.demo.mapper;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import com.example.demo.dto.PaymentRequestDTO;
import com.example.demo.dto.PaymentResponseDTO;
import com.example.demo.entity.Payment;

@Component
public class PaymentMapper {

    // Convert RequestDTO -> Entity
    public Payment toEntity(PaymentRequestDTO dto) {

        Payment payment = new Payment();

        payment.setOrderId(dto.getOrderId());
        payment.setPaymentMethod(dto.getPaymentMethod());
        payment.setAmount(dto.getAmount());

        payment.setUpiId(dto.getUpiId());
        payment.setBankName(dto.getBankName());

        payment.setCurrency("INR");

        payment.setPaymentDate(LocalDateTime.now());

        return payment;
    }

    // Convert Entity -> ResponseDTO
    public PaymentResponseDTO toResponse(Payment payment) {

        PaymentResponseDTO response = new PaymentResponseDTO();

        response.setPaymentId(payment.getPaymentId());
        response.setOrderId(payment.getOrderId());
        response.setPaymentMethod(payment.getPaymentMethod());
        response.setPaymentStatus(payment.getPaymentStatus());
        response.setAmount(payment.getAmount());
        response.setTransactionRef(payment.getTransactionRef());
        response.setPaymentDate(payment.getPaymentDate());

        return response;
    }

}