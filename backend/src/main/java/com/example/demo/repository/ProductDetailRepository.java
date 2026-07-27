package com.example.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.emart.entity.ProdDtlMaster;

@Repository
public interface ProductDetailRepository extends JpaRepository<ProdDtlMaster, Integer> {

}