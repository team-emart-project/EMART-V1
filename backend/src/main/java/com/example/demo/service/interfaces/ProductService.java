package com.example.demo.service.interfaces;

import com.example.demo.dto.response.ProductResponse;
import com.example.demo.dto.response.ProductVariantResponse;
import com.example.demo.response.PageResponse;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

/** Read-only product browsing (Module 5). */
public interface ProductService {

    /**
     * Products in a category.
     * @param includeSubCategories when true, also returns products filed under
     *                             descendant categories, so "Electronics" is not empty
     *                             just because every product sits under "DSLR".
     */
    PageResponse<ProductResponse> getProductsByCategory(Integer catmasterId,
                                                        boolean includeSubCategories,
                                                        Pageable pageable);

    PageResponse<ProductResponse> searchProducts(String search,
                                                 BigDecimal minPrice,
                                                 BigDecimal maxPrice,
                                                 Pageable pageable);

    /** Detail view, variants included. */
    ProductResponse getProduct(Integer prodId);

    List<ProductVariantResponse> getProductVariants(Integer prodId);
}
