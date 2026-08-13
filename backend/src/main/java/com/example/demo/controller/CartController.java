package com.example.demo.controller;

import com.example.demo.dto.request.CartItemRequest;
import com.example.demo.dto.request.UpdateCartItemRequest;
import com.example.demo.dto.response.CartResponse;
import com.example.demo.response.ApiResponse;
import com.example.demo.service.interfaces.CartService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Module 6 — Cart Management.
 *
 * The controller stays thin on purpose: validate the request, delegate to the
 * service, wrap the result. No business logic and no database access here.
 *
 * Every endpoint acts on the CURRENT user's cart — there is no cartId or userId
 * in any URL, which makes it impossible to touch someone else's cart.
 */
@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    /** GET /api/cart — current user's cart with totals. */
    @GetMapping
    public ResponseEntity<ApiResponse<CartResponse>> getCart() {
        CartResponse cart = cartService.getCart();
        return ResponseEntity.ok(ApiResponse.success("Cart retrieved successfully", cart));
    }

    /** POST /api/cart/items — add a product (201 Created). */
    @PostMapping("/items")
    public ResponseEntity<ApiResponse<CartResponse>> addItem(
            @Valid @RequestBody CartItemRequest request) {

        CartResponse cart = cartService.addItem(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Item added to cart", cart));
    }

    /** PUT /api/cart/items/{cartItemId} — change quantity / points on one line. */
    @PutMapping("/items/{cartItemId}")
    public ResponseEntity<ApiResponse<CartResponse>> updateItem(
            @PathVariable Integer cartItemId,
            @Valid @RequestBody UpdateCartItemRequest request) {

        CartResponse cart = cartService.updateItem(cartItemId, request);
        return ResponseEntity.ok(ApiResponse.success("Cart item updated", cart));
    }

    /** DELETE /api/cart/items/{cartItemId} — remove one line. */
    @DeleteMapping("/items/{cartItemId}")
    public ResponseEntity<ApiResponse<CartResponse>> removeItem(@PathVariable Integer cartItemId) {
        CartResponse cart = cartService.removeItem(cartItemId);
        return ResponseEntity.ok(ApiResponse.success("Cart item removed", cart));
    }

    /** DELETE /api/cart — empty the cart. */
    @DeleteMapping
    public ResponseEntity<ApiResponse<CartResponse>> clearCart() {
        CartResponse cart = cartService.clearCart();
        return ResponseEntity.ok(ApiResponse.success("Cart cleared", cart));
    }
}
