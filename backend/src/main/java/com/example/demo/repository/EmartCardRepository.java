package com.example.demo.repository;

import com.example.demo.entity.EmartCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmartCardRepository extends JpaRepository<EmartCard, Integer> {

    Optional<EmartCard> findByUser_UserId(Integer userId);

    boolean existsByUser_UserId(Integer userId);

    boolean existsByCardNumber(String cardNumber);
}
