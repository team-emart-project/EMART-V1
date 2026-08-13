package com.example.demo.repository;

import com.example.demo.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AddressRepository extends JpaRepository<Address, Integer> {

    List<Address> findByUser_UserIdOrderByIsDefaultDescAddressIdAsc(Integer userId);

    Optional<Address> findByUser_UserIdAndIsDefaultTrue(Integer userId);

    long countByUser_UserId(Integer userId);

    /**
     * Clears the existing default in one UPDATE.
     *
     * @Modifying is required for any non-SELECT @Query. clearAutomatically makes
     * Hibernate flush the persistence context so a stale cached entity cannot
     * overwrite this change afterwards.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Address a SET a.isDefault = false WHERE a.user.userId = :userId AND a.isDefault = true")
    void clearDefaultForUser(@Param("userId") Integer userId);
}
