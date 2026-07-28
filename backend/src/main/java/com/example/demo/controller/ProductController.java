package com.example.demo.controller;

import com.example.demo.dto.response.ApiResponse;
import com.example.demo.dto.response.PageResponse;
import com.example.demo.dto.response.ProductResponse;
import com.example.demo.exception.BadRequestException;
import com.example.demo.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    // GET /api/products
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> getAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageResponse<ProductResponse> products = productService.getAllProducts(page, size);
        return ResponseEntity.ok(ApiResponse.success("Products fetched successfully", products));
    }

    // GET /api/products/filter?brand=&price=&catmasterId=
    @GetMapping("/filter")
    public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> filterProducts(
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) BigDecimal price,
            @RequestParam(required = false) Integer catmasterId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageResponse<ProductResponse> products =
                productService.filterProducts(brand, price, catmasterId, page, size);
        return ResponseEntity.ok(ApiResponse.success("Filtered products fetched successfully", products));
    }

    // GET /api/products/search?q=
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> searchProducts(
            @RequestParam(name = "q") String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (query == null || query.isBlank()) {
            throw new BadRequestException("Search query 'q' must not be empty");
        }
        PageResponse<ProductResponse> products = productService.searchProducts(query, page, size);
        return ResponseEntity.ok(ApiResponse.success("Search results fetched successfully", products));
    }

    // GET /api/products/{id}
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductById(@PathVariable Long id) {
        ProductResponse product = productService.getProductById(id);
        return ResponseEntity.ok(ApiResponse.success("Product fetched successfully", product));
    }
}
