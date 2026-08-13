package com.example.demo.mapper;

import com.example.demo.dto.response.PaymentResponse;
import com.example.demo.entity.Payment;
import org.springframework.stereotype.Component;

@Component
public class PaymentMapper {

    public PaymentResponse toResponse(Payment payment) {
        return PaymentResponse.builder()
                .paymentId(payment.getPaymentId())
                .orderId(payment.getOrder().getOrderId())
                .orderNo(payment.getOrder().getOrderNo())
                .paymentMethod(payment.getPaymentMethod())
                .cardLast4(payment.getCardLast4())
                .amount(payment.getAmount())
                .status(payment.getStatus().name())
                .transactionRef(payment.getTransactionRef())
                .transactionDate(payment.getTransactionDate())
                .orderStatus(payment.getOrder().getOrderStatus().name())
                .build();
    }
}
