package com.example.demo.repository;

import com.example.demo.entity.Orders;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrdersRepository extends JpaRepository<Orders, Integer> {

    Page<Orders> findByUser_UserIdOrderByOrderDateDesc(Integer userId, Pageable pageable);

    /** Order + lines + product names in one query, for the detail/invoice view. */
    @Query("""
            SELECT DISTINCT o FROM Orders o
            LEFT JOIN FETCH o.items i
            LEFT JOIN FETCH i.product
            WHERE o.orderId = :orderId
            """)
    Optional<Orders> findByIdWithItems(@Param("orderId") Integer orderId);

    boolean existsByOrderNo(String orderNo);
}
