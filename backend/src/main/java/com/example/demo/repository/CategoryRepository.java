package com.example.demo.repository;

import com.example.demo.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Integer> {

    // Top-level categories only (no parent sub-cat id set)
    List<Category> findBySubCatIdIsNullAndFlagFalse();

    // Sub-categories belonging to a given parent category code
    List<Category> findByCatIdAndSubCatIdIsNotNullAndFlagFalse(String catId);

    List<Category> findByFlagFalse();
}
