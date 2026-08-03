package com.example.demo.controller;

import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.emart.entity.ProductMaster;
import com.emart.service.ProductService;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public List<ProductMaster> getAllProducts() {
        return productService.getAllProducts();
    }

    @GetMapping("/{id}")
    public ProductMaster getProductById(@PathVariable Integer id) {
        return productService.getProductById(id);
    }
}