package com.example.repository;

import com.example.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Integer> {

    /** Used to decide "increment quantity" vs "insert a new line". */
    Optional<CartItem> findByCart_CartIdAndProduct_ProdId(Integer cartId, Integer prodId);
}
