package com.example.demo.controller;

import com.example.demo.dto.response.ProductResponse;
import com.example.demo.dto.response.ProductVariantResponse;
import com.example.demo.response.ApiResponse;
import com.example.demo.response.PageResponse;
import com.example.demo.service.interfaces.ProductService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * Module 5 — product browsing. Public, read-only.
 *
 * There is deliberately no brand filter: product_master has no brand column,
 * so it cannot be supported without a schema change.
 */
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    /** GET /api/products?search=canon&minPrice=1000&maxPrice=50000&page=0&size=12 */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> searchProducts(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @PageableDefault(size = 12, sort = "prodName", direction = Sort.Direction.ASC) Pageable pageable) {

        PageResponse<ProductResponse> products =
                productService.searchProducts(search, minPrice, maxPrice, pageable);

        return ResponseEntity.ok(ApiResponse.success("Products retrieved successfully", products));
    }

    /** GET /api/products/{prodId} — detail, variants included. */
    @GetMapping("/{prodId}")
    public ResponseEntity<ApiResponse<ProductResponse>> getProduct(@PathVariable Integer prodId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Product retrieved successfully", productService.getProduct(prodId)));
    }

    /** GET /api/products/{prodId}/variants — variants alone, grouped by attribute. */
    @GetMapping("/{prodId}/variants")
    public ResponseEntity<ApiResponse<List<ProductVariantResponse>>> getProductVariants(
            @PathVariable Integer prodId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Product variants retrieved successfully", productService.getProductVariants(prodId)));
    }
}
