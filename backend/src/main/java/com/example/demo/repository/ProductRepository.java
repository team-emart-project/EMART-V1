package com.example.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.emart.entity.ProductMaster;

@Repository
public interface ProductRepository extends JpaRepository<ProductMaster, Integer> {

}