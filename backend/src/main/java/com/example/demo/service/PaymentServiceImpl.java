package com.example.demo.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.dto.PaymentRequestDTO;
import com.example.demo.dto.PaymentResponseDTO;
import com.example.demo.entity.Payment;
import com.example.demo.mapper.PaymentMapper;
import com.example.demo.repository.PaymentRepository;

@Service
public class PaymentServiceImpl implements PaymentService {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PaymentMapper paymentMapper;

    @Override
    public PaymentResponseDTO makePayment(PaymentRequestDTO request) {
    	if (request.getOrderId() == null) {
    	    throw new RuntimeException("Order Id is Required");
    	}
    	

        Payment payment = paymentMapper.toEntity(request);

        // Generate Transaction Reference
        String txnRef = "EMART"
                + System.currentTimeMillis();

        payment.setTransactionRef(txnRef);

        payment.setCurrency("INR");
        payment.setPaymentDate(LocalDateTime.now());

        String method = request.getPaymentMethod().toUpperCase();

        switch (method) {

        case "CARD":

            if (request.getCardNumber() != null
                    && request.getCardNumber().length() >= 4) {

                String last4 = request.getCardNumber()
                        .substring(request.getCardNumber().length() - 4);

                payment.setCardLast4(last4);
            }

            payment.setPaymentProvider("CARD");
            payment.setPaymentStatus("SUCCESS");
            break;

        case "UPI":

            payment.setUpiId(request.getUpiId());
            payment.setPaymentProvider("UPI");
            payment.setPaymentStatus("SUCCESS");
            break;

        case "NETBANKING":

            payment.setBankName(request.getBankName());
            payment.setPaymentProvider("NETBANKING");
            payment.setPaymentStatus("SUCCESS");
            break;

        case "WALLET":

            payment.setPaymentProvider(request.getWalletName());
            payment.setPaymentStatus("SUCCESS");
            break;

        case "COD":

            payment.setPaymentProvider("COD");
            payment.setPaymentStatus("PENDING");
            break;

        default:
            throw new RuntimeException("Invalid Payment Method");
        }

        Payment savedPayment = paymentRepository.save(payment);

        PaymentResponseDTO response =
                paymentMapper.toResponse(savedPayment);

        response.setMessage("Payment Completed Successfully");

        return response;
    }

    @Override
    public PaymentResponseDTO getPaymentById(Integer paymentId) {

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() ->
                        new RuntimeException("Payment Not Found"));

        return paymentMapper.toResponse(payment);
    }

    @Override
    public List<PaymentResponseDTO> getAllPayments() {

        return paymentRepository.findAll()
                .stream()
                .map(paymentMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<PaymentResponseDTO> getPaymentsByOrderId(Integer orderId) {

        return paymentRepository.findByOrderId(orderId)
                .stream()
                .map(paymentMapper::toResponse)
                .collect(Collectors.toList());
    }

}