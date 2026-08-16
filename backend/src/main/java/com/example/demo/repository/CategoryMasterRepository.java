package com.example.demo.repository;

import com.example.demo.entity.CategoryMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryMasterRepository extends JpaRepository<CategoryMaster, Integer> {

    /**
     * Root categories. In the seed data a root row carries subcat_id = '^'
     * (the teacher's convention for "no parent").
     */
    List<CategoryMaster> findBySubcatIdOrderByCatNameAsc(String subcatId);

    /** Children of a category: rows whose subcat_id equals the parent's cat_id. */
    List<CategoryMaster> findBySubcatIdIgnoreCaseOrderByCatNameAsc(String parentCatId);

    boolean existsByCatId(String catId);

    /** Module 1 (Home): categories marked flag = 1 for the home-page tiles. */
    @Query("SELECT c FROM CategoryMaster c WHERE c.flag = true ORDER BY c.catName ASC")
    List<CategoryMaster> findByFlagTrueOrderByCatNameAsc();
}
