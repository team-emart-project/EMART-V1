package com.example.demo.repository;

import com.example.demo.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Integer> {

    /** Derived query — Spring Data builds the SQL from the method name. */
    Optional<Cart> findByUser_UserId(Integer userId);

    /**
     * Custom query with JOIN FETCH: loads the cart, its items and each item's
     * product in ONE select. Without this, rendering a 10-line cart would fire
     * 1 query for the cart plus 10 more for the products (the N+1 problem).
     */
    @Query("""
            SELECT DISTINCT c FROM Cart c
            LEFT JOIN FETCH c.items i
            LEFT JOIN FETCH i.product p
            WHERE c.user.userId = :userId
            """)
    Optional<Cart> findByUserIdWithItems(@Param("userId") Integer userId);
}
