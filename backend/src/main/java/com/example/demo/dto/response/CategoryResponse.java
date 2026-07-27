package com.example.demo.dto.response;

import com.example.demo.entity.Category;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryResponse {
    private Integer catmasterId;
    private String catId;
    private String subCatId;
    private String categoryName;
    private String catImagePath;

    public static CategoryResponse from(Category category) {
        return CategoryResponse.builder()
                .catmasterId(category.getCatmasterId())
                .catId(category.getCatId())
                .subCatId(category.getSubCatId())
                .categoryName(category.getCategoryName())
                .catImagePath(category.getCatImagePath())
                .build();
    }
}
