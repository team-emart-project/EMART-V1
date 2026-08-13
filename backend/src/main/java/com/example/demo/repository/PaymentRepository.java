package com.example.demo.repository;

import com.example.demo.entity.Payment;
import com.example.demo.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Integer> {

    /** Every attempt on an order, newest first — failed ones are kept. */
    List<Payment> findByOrder_OrderIdOrderByTransactionDateDesc(Integer orderId);

    Optional<Payment> findFirstByOrder_OrderIdAndStatusOrderByTransactionDateDesc(
            Integer orderId, PaymentStatus status);

    boolean existsByOrder_OrderIdAndStatus(Integer orderId, PaymentStatus status);
}
