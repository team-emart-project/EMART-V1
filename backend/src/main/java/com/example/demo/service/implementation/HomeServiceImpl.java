package com.example.demo.service.implementation;

import com.example.demo.dto.response.CategoryResponse;
import com.example.demo.dto.response.ProductResponse;
import com.example.demo.mapper.CategoryMapper;
import com.example.demo.mapper.ProductMapper;
import com.example.demo.repository.CategoryMasterRepository;
import com.example.demo.repository.ProductMasterRepository;
import com.example.demo.service.CardholderService;
import com.example.demo.service.interfaces.HomeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Home page.
 *
 * This module owns no tables and no entities of its own — it reuses Module 5's
 * repositories and mappers. Duplicating them would mean two places to fix when
 * the catalog changes.
 */
@Service
@Transactional(readOnly = true)
public class HomeServiceImpl implements HomeService {

    private static final Logger log = LoggerFactory.getLogger(HomeServiceImpl.class);

    private static final int MAX_LIMIT = 50;

    private final CategoryMasterRepository categoryRepository;
    private final ProductMasterRepository productRepository;
    private final CategoryMapper categoryMapper;
    private final ProductMapper productMapper;
    private final CardholderService cardholderService;

    public HomeServiceImpl(CategoryMasterRepository categoryRepository,
                           ProductMasterRepository productRepository,
                           CategoryMapper categoryMapper,
                           ProductMapper productMapper,
                           CardholderService cardholderService) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.categoryMapper = categoryMapper;
        this.productMapper = productMapper;
        this.cardholderService = cardholderService;
    }

    /**
     * "Featured" = category_master.flag = 1.
     *
     * That column is the teacher's own marker for a row that should jump
     * straight to a product rather than to another level of categories — which
     * is exactly what a home-page tile does.
     */
    @Override
    public List<CategoryResponse> getFeaturedCategories() {
        return categoryRepository.findByFlagTrueOrderByCatNameAsc().stream()
                .map(categoryMapper::toResponse)
                .toList();
    }

    @Override
    public List<ProductResponse> getNewArrivals(int limit) {
        // Clamp rather than reject: a silly ?limit=100000 should not 500,
        // and it must not let a caller pull the whole catalog in one request.
        int safeLimit = Math.max(1, Math.min(limit, MAX_LIMIT));

        // Public endpoint: false for a signed-out visitor, so cardholder_price
        // is stripped from the payload for anyone without an active card.
        boolean cardholder = cardholderService.isCurrentUserCardholder();

        log.debug("New arrivals: limit={} cardholder={}", safeLimit, cardholder);

        return productRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, safeLimit)).stream()
                .map(p -> productMapper.toSummary(p, cardholder))
                .toList();
    }
}
