package com.example.demo.service;

import java.util.List;
import com.emart.entity.ProductMaster;
import org.springframework.stereotype.Service;

import com.emart.entity.ProductMaster;
import com.emart.repository.ProductRepository;
import com.emart.service.ProductService;

public interface ProductService {

    List<ProductMaster> getAllProducts();

    ProductMaster getProductById(Integer id);

}