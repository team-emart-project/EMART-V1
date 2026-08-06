package com.example.demo.controller;

import com.example.demo.dto.response.HomePageResponse;
import com.example.demo.service.HomeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/home")
public class HomeController {

    private final HomeService homeService;

    public HomeController(HomeService homeService) {
        this.homeService = homeService;
    }


    // GET /api/home – single call to render the storefront homepage:
    // active banners, categories, featured products, new arrivals, best sellers
    @GetMapping
    public ResponseEntity<HomePageResponse> getHomePageData() {
        return ResponseEntity.ok(homeService.getHomePageData());
    }
}
