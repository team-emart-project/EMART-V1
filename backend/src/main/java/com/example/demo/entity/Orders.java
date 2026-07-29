package com.example.demo.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.*;

@Entity
@Table(name = "orders")
public class Orders {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Integer orderId;

    @Column(name = "order_no")
    private String orderNo;

    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "shipping_address_id")
    private Integer shippingAddressId;

    @Column(name = "billing_address_id")
    private Integer billingAddressId;

    @Column(name = "subtotal_amount")
    private BigDecimal subtotalAmount;

    @Column(name = "tax_amount")
    private BigDecimal taxAmount;

    @Column(name = "total_amount")
    private Double totalAmount;

    @Column(name = "points_redeemed")
    private Integer pointsRedeemed;

    @Column(name = "points_earned")
    private Integer pointsEarned;

    @Column(name = "payment_status")
    private String paymentStatus;

    @Column(name = "order_status")
    private String orderStatus;

    @Column(name = "customer_id")
    private Integer customerId;

    @Column(name = "delivery_address")
    private String deliveryAddress;

    @Column(name = "order_date")
    private LocalDateTime orderDate;

    // Generate Getters & Setters using Eclipse:
    // Source → Generate Getters and Setters
}