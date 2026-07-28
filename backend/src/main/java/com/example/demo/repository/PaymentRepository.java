package com.example.demo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.entity.Payment;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Integer> {

    // Find payment using transaction reference
    Optional<Payment> findByTransactionRef(String transactionRef);

    // Find all payments of a particular order
    List<Payment> findByOrderId(Integer orderId);

    // Find all payments by status
    List<Payment> findByPaymentStatus(String paymentStatus);

}