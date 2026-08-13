package com.example.demo.repository;

import com.example.demo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    Optional<User> findByEmail(String email);

    /**
     * Looks a user up by Google's immutable account id ("sub").
     *
     * Tried BEFORE findByEmail during Google sign-in: a Google account's email
     * address can change, its sub cannot, so this is the stable link.
     */
    Optional<User> findByGoogleSub(String googleSub);

    boolean existsByEmail(String email);

    boolean existsByMembershipNo(String membershipNo);

    /** Used by reset-password; the token is a random UUID, unique in practice. */
    Optional<User> findByResetPasswordToken(String resetPasswordToken);
}
