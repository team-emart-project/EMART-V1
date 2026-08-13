package com.example.demo.service.interfaces;

import com.example.demo.dto.request.CheckoutRequest;
import com.example.demo.dto.response.OrderResponse;
import com.example.demo.response.PageResponse;
import org.springframework.data.domain.Pageable;

/** Module 8 — checkout and order history. */
public interface OrderService {

    /** Calculates totals WITHOUT saving anything. */
    OrderResponse checkoutPreview(CheckoutRequest request);

    OrderResponse placeOrder(CheckoutRequest request);

    PageResponse<OrderResponse> getMyOrders(Pageable pageable);

    OrderResponse getOrder(Integer orderId);

    OrderResponse cancelOrder(Integer orderId);

    /** Invoice PDF bytes. Only available once the order is PAID. */
    byte[] generateInvoicePdf(Integer orderId);
}
