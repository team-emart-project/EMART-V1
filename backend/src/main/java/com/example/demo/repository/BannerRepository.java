package com.example.demo.repository;

import com.example.demo.entity.base.Banner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface BannerRepository extends JpaRepository<Banner, Long> {

    List<Banner> findAllByOrderByDisplayOrderAsc();

    @Query("SELECT b FROM Banner b WHERE b.active = true " +
            "AND (b.startDate IS NULL OR b.startDate <= :now) " +
            "AND (b.endDate IS NULL OR b.endDate >= :now) " +
            "ORDER BY b.displayOrder ASC")
    List<Banner> findActiveBanners(@Param("now") LocalDateTime now);
}
