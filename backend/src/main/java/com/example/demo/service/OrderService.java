package com.example.demo.service;

import java.util.List;

import com.example.demo.dto.OrderRequestDTO;
import com.example.demo.dto.OrderResponseDTO;

public interface OrderService {

    OrderResponseDTO createOrder(OrderRequestDTO request);

    OrderResponseDTO getOrderById(Integer orderId);

    List<OrderResponseDTO> getAllOrders();

}