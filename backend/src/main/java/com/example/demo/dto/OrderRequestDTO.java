package com.example.demo.dto;

import java.math.BigDecimal;

public class OrderRequestDTO {

    private Integer userId;
    private Integer customerId;

    private Integer shippingAddressId;
    private Integer billingAddressId;

    private BigDecimal subtotalAmount;
    private BigDecimal taxAmount;

    private Double totalAmount;

    private String deliveryAddress;

    // Generate Getters & Setters
}