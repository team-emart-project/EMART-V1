package com.example.demo.service.interfaces;

import com.example.demo.dto.response.CategoryResponse;
import com.example.demo.dto.response.ProductResponse;

import java.util.List;

/**
 * Module 1 — Home page content.
 *
 * Built entirely from category_master and product_master. The BRD also asks for
 * promotional banners and sponsor adverts, but the schema has no table for
 * those, so they are not implemented.
 */
public interface HomeService {

    /** Categories flagged for direct display (category_master.flag = 1). */
    List<CategoryResponse> getFeaturedCategories();

    /** Most recently added products. */
    List<ProductResponse> getNewArrivals(int limit);
}
