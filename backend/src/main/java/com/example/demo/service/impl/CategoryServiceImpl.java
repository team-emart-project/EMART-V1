package com.example.demo.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.emart.entity.CategoryMaster;
import com.emart.repository.CategoryRepository;
import com.emart.service.CategoryService;

@Service
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryServiceImpl(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    public List<CategoryMaster> getAllCategories() {
        return categoryRepository.findAll();
    }

    @Override
    public CategoryMaster getCategoryById(Integer id) {
        return categoryRepository.findById(id).orElse(null);
    }
}