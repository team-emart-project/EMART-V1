package com.emart.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.emart.dto.request.CheckoutRequest;
import com.emart.dto.response.OrderResponse;
import com.emart.service.OrderService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    // Checkout Preview
    @PostMapping("/checkout-preview")
    public ResponseEntity<OrderResponse> checkoutPreview(
            @Valid @RequestBody CheckoutRequest request) {

        return ResponseEntity.ok(orderService.checkoutPreview(request));
    }

    // Place Order
    @PostMapping
    public ResponseEntity<OrderResponse> placeOrder(
            @Valid @RequestBody CheckoutRequest request) {

        return ResponseEntity.ok(orderService.placeOrder(request));
    }

    // Get My Orders
    @GetMapping
    public ResponseEntity<Page<OrderResponse>> getMyOrders(Pageable pageable) {

        return ResponseEntity.ok(orderService.getMyOrders(pageable));
    }

    // Get Single Order
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrder(
            @PathVariable Integer orderId) {

        return ResponseEntity.ok(orderService.getOrder(orderId));
    }

    // Cancel Order
    @PutMapping("/{orderId}/cancel")
    public ResponseEntity<String> cancelOrder(
            @PathVariable Integer orderId) {

        orderService.cancelOrder(orderId);

        return ResponseEntity.ok("Order Cancelled Successfully");
    }

    // Download Invoice
    @GetMapping("/{orderId}/invoice-pdf")
    public ResponseEntity<byte[]> downloadInvoice(
            @PathVariable Integer orderId) {

        byte[] pdf = orderService.generateInvoicePdf(orderId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=invoice.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}