package com.emart.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.emart.entity.Orders;

public interface OrdersRepository extends JpaRepository<Orders, Integer> {

    Page<Orders> findByUserUserId(Integer userId, Pageable pageable);

    Optional<Orders> findByOrderIdAndUserUserId(Integer orderId, Integer userId);

    Optional<Orders> findByOrderNo(String orderNo);

}