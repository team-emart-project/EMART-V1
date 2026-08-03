package com.example.demo.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryTreeResponse {
    private Integer catmasterId;
    private String catId;
    private String categoryName;
    private String catImagePath;
    private List<CategoryResponse> subCategories;
}
