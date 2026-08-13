package com.example.demo.repository;

import com.example.demo.entity.Wishlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WishlistRepository extends JpaRepository<Wishlist, Integer> {

    /** JOIN FETCH the product so rendering the list is one query, not N+1. */
    @Query("""
            SELECT w FROM Wishlist w
            JOIN FETCH w.product p
            WHERE w.user.userId = :userId
            ORDER BY w.addedAt DESC
            """)
    List<Wishlist> findByUserIdWithProduct(@Param("userId") Integer userId);

    boolean existsByUser_UserIdAndProduct_ProdId(Integer userId, Integer prodId);

    Optional<Wishlist> findByUser_UserIdAndProduct_ProdId(Integer userId, Integer prodId);
}
