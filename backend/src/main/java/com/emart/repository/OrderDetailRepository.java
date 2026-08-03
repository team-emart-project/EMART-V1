package com.emart.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.emart.entity.OrderDetail;

public interface OrderDetailRepository
        extends JpaRepository<OrderDetail, Integer> {

}