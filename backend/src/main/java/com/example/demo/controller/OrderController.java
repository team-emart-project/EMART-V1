package com.example.demo.controller;

import com.example.demo.dto.request.CheckoutRequest;
import com.example.demo.dto.response.OrderResponse;
import com.example.demo.response.ApiResponse;
import com.example.demo.response.PageResponse;
import com.example.demo.service.interfaces.OrderService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Module 8 — Checkout and orders. All endpoints require a logged-in user. */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * POST /api/orders/checkout-preview
     * Calculates totals, savings and points WITHOUT saving anything.
     */
    @PostMapping("/checkout-preview")
    public ResponseEntity<ApiResponse<OrderResponse>> checkoutPreview(
            @Valid @RequestBody CheckoutRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Checkout preview calculated", orderService.checkoutPreview(request)));
    }

    /** POST /api/orders — converts the cart into a real order. */
    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> placeOrder(
            @Valid @RequestBody CheckoutRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                "Order placed successfully. Proceed to payment.",
                orderService.placeOrder(request)));
    }

    /** GET /api/orders?page=0&size=10 */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<OrderResponse>>> getMyOrders(
            @PageableDefault(size = 10, sort = "orderDate", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(
                "Orders retrieved successfully", orderService.getMyOrders(pageable)));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrder(@PathVariable Integer orderId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Order retrieved successfully", orderService.getOrder(orderId)));
    }

    @PutMapping("/{orderId}/cancel")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(@PathVariable Integer orderId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Order cancelled", orderService.cancelOrder(orderId)));
    }

    /**
     * GET /api/orders/{orderId}/invoice-pdf
     *
     * Returns the PDF bytes directly rather than the ApiResponse envelope —
     * a browser or Postman needs the raw file to render or download it.
     */
    @GetMapping(value = "/{orderId}/invoice-pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> downloadInvoice(@PathVariable Integer orderId) {

        byte[] pdf = orderService.generateInvoicePdf(orderId);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"invoice-" + orderId + ".pdf\"")
                .body(pdf);
    }
}
