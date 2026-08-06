package com.example.service.interfaces;

import com.example.dto.request.CartItemRequest;
import com.example.dto.request.UpdateCartItemRequest;
import com.example.dto.response.CartResponse;

/**
 * Cart operations for the CURRENTLY LOGGED-IN user.
 *
 * Note that no method takes a userId — that is resolved from the security
 * context inside the implementation, so a caller can never act on another
 * user's cart.
 */
public interface CartService {

    CartResponse getCart();

    CartResponse addItem(CartItemRequest request);

    CartResponse updateItem(Integer cartItemId, UpdateCartItemRequest request);

    CartResponse removeItem(Integer cartItemId);

    CartResponse clearCart();
}
