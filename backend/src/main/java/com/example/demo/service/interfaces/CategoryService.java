package com.example.demo.service.interfaces;

import com.example.demo.dto.response.CategoryResponse;

import java.util.List;

/** Read-only category browsing (Module 5). */
public interface CategoryService {

    /** Full nested tree, roots first. */
    List<CategoryResponse> getCategoryTree();

    /** Top-level categories only — cheap call for the nav bar. */
    List<CategoryResponse> getRootCategories();

    /** Direct children of one category. */
    List<CategoryResponse> getSubCategories(Integer catmasterId);

    CategoryResponse getCategory(Integer catmasterId);
}
