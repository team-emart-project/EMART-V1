package com.example.demo.repository;

import com.example.demo.entity.ProductMaster;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductMasterRepository extends JpaRepository<ProductMaster, Integer> {

    /** Product detail with its category already loaded — avoids a second query. */
    @Query("""
            SELECT p FROM ProductMaster p
            JOIN FETCH p.category
            WHERE p.prodId = :prodId
            """)
    Optional<ProductMaster> findByIdWithCategory(@Param("prodId") Integer prodId);

    /** All products directly under one category, paginated. */
    Page<ProductMaster> findByCategory_CatmasterId(Integer catmasterId, Pageable pageable);

    /** Products across a set of categories — used to include sub-category products. */
    Page<ProductMaster> findByCategory_CatmasterIdIn(List<Integer> catmasterIds, Pageable pageable);

    /**
     * Search + optional price range.
     *
     * Written as an explicit JPQL query rather than a long derived method name
     * because every filter is optional: a NULL parameter disables that clause.
     * Prices are compared against mrp_price, the list price everyone sees.
     */
    @Query("""
            SELECT p FROM ProductMaster p
            WHERE (:search IS NULL OR LOWER(p.prodName) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:minPrice IS NULL OR p.mrpPrice >= :minPrice)
              AND (:maxPrice IS NULL OR p.mrpPrice <= :maxPrice)
            """)
    Page<ProductMaster> search(@Param("search") String search,
                               @Param("minPrice") BigDecimal minPrice,
                               @Param("maxPrice") BigDecimal maxPrice,
                               Pageable pageable);

    /** Newest products — backs the Home Page "new arrivals" strip (Module 1). */
    List<ProductMaster> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
