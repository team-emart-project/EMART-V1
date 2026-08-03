package com.emart.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.emart.dto.request.CheckoutRequest;
import com.emart.dto.response.OrderResponse;

public interface OrderService {

    // Checkout Preview
    OrderResponse checkoutPreview(CheckoutRequest request);

    // Place Order
    OrderResponse placeOrder(CheckoutRequest request);

    // Get Logged-in User Orders
    Page<OrderResponse> getMyOrders(Pageable pageable);

    // Get Single Order
    OrderResponse getOrder(Integer orderId);

    // Cancel Order
    void cancelOrder(Integer orderId);

    // Download Invoice PDF
    byte[] generateInvoicePdf(Integer orderId);

}