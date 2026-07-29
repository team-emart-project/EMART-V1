package com.example.demo.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.dto.OrderRequestDTO;
import com.example.demo.dto.OrderResponseDTO;
import com.example.demo.entity.Orders;
import com.example.demo.mapper.OrderMapper;
import com.example.demo.repository.OrderRepository;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderMapper orderMapper;

    @Override
    public OrderResponseDTO createOrder(OrderRequestDTO request) {

        Orders order = orderMapper.toEntity(request);

        order.setOrderStatus("PLACED");
        order.setPaymentStatus("PENDING");
        order.setOrderDate(LocalDateTime.now());

        String orderNo = "ORD" + System.currentTimeMillis();
        order.setOrderNo(orderNo);

        System.out.println("Generated Order No : " + order.getOrderNo());

        Orders savedOrder = orderRepository.save(order);

        return orderMapper.toResponse(savedOrder);
    }

    @Override
    public OrderResponseDTO getOrderById(Integer orderId) {

        Orders order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order Not Found"));

        return orderMapper.toResponse(order);
    }

    @Override
    public List<OrderResponseDTO> getAllOrders() {

        return orderRepository.findAll()
                .stream()
                .map(orderMapper::toResponse)
                .collect(Collectors.toList());
    }
}