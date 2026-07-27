package com.example.demo.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.emart.entity.ProductMaster;
import com.emart.repository.ProductRepository;
import com.emart.service.ProductService;

@Service
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

    public ProductServiceImpl(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public List<ProductMaster> getAllProducts() {
        return productRepository.findAll();
    }

    @Override
    public ProductMaster getProductById(Integer id) {
        return productRepository.findById(id).orElse(null);
    }
}