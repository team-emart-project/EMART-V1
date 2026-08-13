package com.example.demo.repository;

import com.example.demo.entity.ConfigMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConfigMasterRepository extends JpaRepository<ConfigMaster, Integer> {
}
