package com.example.demo.service;

import java.util.List;
import com.emart.entity.CategoryMaster;

public interface CategoryService {

    List<CategoryMaster> getAllCategories();

    CategoryMaster getCategoryById(Integer id);

}