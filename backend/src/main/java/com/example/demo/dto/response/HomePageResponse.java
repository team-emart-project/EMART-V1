package com.example.demo.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HomePageResponse {

    private List<BannerDTO> banners;
    private List<CategoryDTO> categories;
    private List<ProductDTO> featuredProducts;
    private List<ProductDTO> newArrivals;
    private List<ProductDTO> bestSellers;
}
