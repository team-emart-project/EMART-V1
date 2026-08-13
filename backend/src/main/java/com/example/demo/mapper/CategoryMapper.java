package com.example.demo.mapper;

import com.example.demo.dto.response.CategoryResponse;
import com.example.demo.entity.CategoryMaster;
import org.springframework.stereotype.Component;

@Component
public class CategoryMapper {

    /** Flat form — no children. Used inside lists. */
    public CategoryResponse toResponse(CategoryMaster category) {
        return CategoryResponse.builder()
                .catmasterId(category.getCatmasterId())
                .catId(category.getCatId())
                .subcatId(category.getSubcatId())
                .catName(category.getCatName())
                .catImagePath(category.getCatImagePath())
                .flag(category.getFlag())
                .build();
    }
}
