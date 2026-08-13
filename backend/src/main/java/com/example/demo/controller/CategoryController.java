package com.example.demo.controller;

import com.example.demo.dto.response.CategoryResponse;
import com.example.demo.dto.response.ProductResponse;
import com.example.demo.response.ApiResponse;
import com.example.demo.response.PageResponse;
import com.example.demo.service.interfaces.CategoryService;
import com.example.demo.service.interfaces.ProductService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Module 5 — category browsing. All endpoints are PUBLIC (see SecurityConfig):
 * a visitor can browse the whole catalog without registering, which is what the
 * BRD asks for. Login is only needed to buy.
 */
@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;
    private final ProductService productService;

    public CategoryController(CategoryService categoryService, ProductService productService) {
        this.categoryService = categoryService;
        this.productService = productService;
    }

    /**
     * GET /api/categories            -> full nested tree (default)
     * GET /api/categories?flat=true  -> root categories only, cheaper for a nav bar
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getCategories(
            @RequestParam(defaultValue = "false") boolean flat) {

        List<CategoryResponse> categories = flat
                ? categoryService.getRootCategories()
                : categoryService.getCategoryTree();

        return ResponseEntity.ok(ApiResponse.success("Categories retrieved successfully", categories));
    }

    @GetMapping("/{catmasterId}")
    public ResponseEntity<ApiResponse<CategoryResponse>> getCategory(@PathVariable Integer catmasterId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Category retrieved successfully", categoryService.getCategory(catmasterId)));
    }

    @GetMapping("/{catmasterId}/subcategories")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getSubCategories(
            @PathVariable Integer catmasterId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Sub-categories retrieved successfully", categoryService.getSubCategories(catmasterId)));
    }

    /**
     * GET /api/categories/{id}/products?includeSubCategories=true&page=0&size=12
     *
     * includeSubCategories defaults to TRUE because products are filed against
     * leaf categories — asking for "Electronics" without it returns nothing.
     */
    @GetMapping("/{catmasterId}/products")
    public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> getProductsInCategory(
            @PathVariable Integer catmasterId,
            @RequestParam(defaultValue = "true") boolean includeSubCategories,
            @PageableDefault(size = 12, sort = "prodName", direction = Sort.Direction.ASC) Pageable pageable) {

        PageResponse<ProductResponse> products =
                productService.getProductsByCategory(catmasterId, includeSubCategories, pageable);

        return ResponseEntity.ok(ApiResponse.success("Products retrieved successfully", products));
    }
}
