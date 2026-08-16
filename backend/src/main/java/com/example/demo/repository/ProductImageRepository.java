package com.example.demo.repository;

import com.example.demo.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductImageRepository extends JpaRepository<ProductImage, Integer> {

    List<ProductImage> findByProduct_ProdIdOrderByDisplayOrderAsc(Integer prodId);

    /**
     * Images for a whole page of products in ONE query.
     *
     * Without this, rendering a 12-product grid would fire 12 extra selects —
     * the N+1 problem. The service groups the result by prodId in memory.
     */
    @Query("""
            SELECT i FROM ProductImage i
            WHERE i.product.prodId IN :prodIds
            ORDER BY i.product.prodId ASC, i.displayOrder ASC
            """)
    List<ProductImage> findByProductIds(@Param("prodIds") List<Integer> prodIds);
}
