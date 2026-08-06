package com.example.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.emart.entity.CategoryMaster;
//import com.emart.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;   // if using @Autowired

@Repository
public interface CategoryRepository extends JpaRepository<CategoryMaster, Integer> {

}