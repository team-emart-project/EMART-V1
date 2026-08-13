package com.example.demo.repository;

import com.example.demo.entity.ProdDtlMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProdDtlMasterRepository extends JpaRepository<ProdDtlMaster, Integer> {

    /**
     * JOIN FETCH the config so grouping by attribute name does not fire one
     * extra query per variant row (the N+1 problem).
     */
    @Query("""
            SELECT d FROM ProdDtlMaster d
            JOIN FETCH d.config c
            WHERE d.product.prodId = :prodId
            ORDER BY c.configName ASC, d.configDtls ASC
            """)
    List<ProdDtlMaster> findByProductIdWithConfig(@Param("prodId") Integer prodId);
}
