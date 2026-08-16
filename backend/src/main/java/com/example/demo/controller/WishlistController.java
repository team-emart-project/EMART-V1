package com.example.demo.controller;

import com.example.demo.dto.request.WishlistRequest;
import com.example.demo.dto.response.WishlistResponse;
import com.example.demo.response.ApiResponse;
import com.example.demo.service.interfaces.WishlistService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Module 7 — Wishlist. All endpoints require a logged-in user. */
@RestController
@RequestMapping("/api/wishlist")
public class WishlistController {

    private final WishlistService wishlistService;

    public WishlistController(WishlistService wishlistService) {
        this.wishlistService = wishlistService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<WishlistResponse>>> getMyWishlist() {
        return ResponseEntity.ok(ApiResponse.success(
                "Wishlist retrieved successfully", wishlistService.getMyWishlist()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<WishlistResponse>> addToWishlist(
            @Valid @RequestBody WishlistRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                "Product added to wishlist", wishlistService.addToWishlist(request)));
    }

    @DeleteMapping("/{wishlistId}")
    public ResponseEntity<ApiResponse<Void>> removeFromWishlist(@PathVariable Integer wishlistId) {
        wishlistService.removeFromWishlist(wishlistId);
        return ResponseEntity.ok(ApiResponse.success("Product removed from wishlist"));
    }
}
