package com.example.demo.controller;

import com.example.demo.dto.response.CategoryResponse;
import com.example.demo.dto.response.ProductResponse;
import com.example.demo.response.ApiResponse;
import com.example.demo.service.interfaces.HomeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Module 1 — Home page. Public: this is the landing page, so it must render
 * for a visitor who has never registered.
 */
@RestController
@RequestMapping("/api/home")
public class HomeController {
    public HomeController(HomeService homeService) {
        this.homeService = homeService;
    }


    private final HomeService homeService;

    /** GET /api/home/featured-categories */
    @GetMapping("/featured-categories")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getFeaturedCategories() {
        return ResponseEntity.ok(ApiResponse.success(
                "Featured categories retrieved successfully",
                homeService.getFeaturedCategories()));
    }

    /** GET /api/home/new-arrivals?limit=8 */
    @GetMapping("/new-arrivals")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getNewArrivals(
            @RequestParam(defaultValue = "8") int limit) {
        return ResponseEntity.ok(ApiResponse.success(
                "New arrivals retrieved successfully",
                homeService.getNewArrivals(limit)));
    }
}
