package com.example.demo.service;

import com.example.demo.dto.response.CategoryResponse;
import com.example.demo.dto.response.CategoryTreeResponse;
import com.example.demo.dto.response.PageResponse;
import com.example.demo.dto.response.ProductResponse;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.entity.Category;
import com.example.demo.repository.CategoryRepository;
import com.example.demo.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    // GET /api/categories -> flat list of all active categories
    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findByFlagFalse()
                .stream()
                .map(CategoryResponse::from)
                .collect(Collectors.toList());
    }

    // GET /api/categories/tree -> top-level categories, each with its sub-categories nested
    public List<CategoryTreeResponse> getCategoryTree() {
        List<Category> topLevel = categoryRepository.findBySubCatIdIsNullAndFlagFalse();

        return topLevel.stream()
                .sorted(Comparator.comparing(Category::getCategoryName))
                .map(parent -> CategoryTreeResponse.builder()
                        .catmasterId(parent.getCatmasterId())
                        .catId(parent.getCatId())
                        .categoryName(parent.getCategoryName())
                        .catImagePath(parent.getCatImagePath())
                        .subCategories(categoryRepository
                                .findByCatIdAndSubCatIdIsNotNullAndFlagFalse(parent.getCatId())
                                .stream()
                                .map(CategoryResponse::from)
                                .collect(Collectors.toList()))
                        .build())
                .collect(Collectors.toList());
    }

    // GET /api/categories/{id}/products -> all products under a category, paginated
    public PageResponse<ProductResponse> getProductsByCategory(Integer catmasterId, int page, int size) {
        if (!categoryRepository.existsById(catmasterId)) {
            throw new ResourceNotFoundException("Category not found with id: " + catmasterId);
        }
        Pageable pageable = PageRequest.of(page, size);
        Page<ProductResponse> result = productRepository
                .findByCategory_CatmasterId(catmasterId, pageable)
                .map(ProductResponse::from);
        return PageResponse.from(result);
    }
}
