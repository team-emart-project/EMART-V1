package com.example.demo.service.implementation;

import com.example.demo.dto.response.ProductResponse;
import com.example.demo.dto.response.ProductVariantResponse;
import com.example.demo.entity.CategoryMaster;
import com.example.demo.entity.ProdDtlMaster;
import com.example.demo.entity.ProductImage;
import com.example.demo.entity.ProductMaster;
import com.example.demo.exception.BusinessRuleViolationException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.mapper.ProductMapper;
import com.example.demo.repository.CategoryMasterRepository;
import com.example.demo.repository.ProdDtlMasterRepository;
import com.example.demo.repository.ProductImageRepository;
import com.example.demo.repository.ProductMasterRepository;
import com.example.demo.response.PageResponse;
import com.example.demo.service.CardholderService;
import com.example.demo.service.interfaces.ProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ProductServiceImpl implements ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductServiceImpl.class);

    private static final String ROOT_MARKER = "^";
    private static final int MAX_DEPTH = 10;

    private final ProductMasterRepository productRepository;
    private final CategoryMasterRepository categoryRepository;
    private final ProdDtlMasterRepository prodDtlRepository;
    private final ProductImageRepository productImageRepository;
    private final ProductMapper productMapper;
    private final CardholderService cardholderService;

    public ProductServiceImpl(ProductMasterRepository productRepository,
                              CategoryMasterRepository categoryRepository,
                              ProdDtlMasterRepository prodDtlRepository,
                              ProductImageRepository productImageRepository,
                              ProductMapper productMapper,
                              CardholderService cardholderService) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.prodDtlRepository = prodDtlRepository;
        this.productImageRepository = productImageRepository;
        this.productMapper = productMapper;
        this.cardholderService = cardholderService;
    }

    @Override
    public PageResponse<ProductResponse> getProductsByCategory(Integer catmasterId,
                                                               boolean includeSubCategories,
                                                               Pageable pageable) {

        CategoryMaster category = categoryRepository.findById(catmasterId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "catmasterId", catmasterId));

        Page<ProductMaster> page;

        if (includeSubCategories) {
            // Products are filed against LEAF categories, so asking for
            // "Electronics" directly would return nothing. Collect the whole
            // branch and query across all of it.
            List<Integer> branch = collectBranchIds(category);
            page = productRepository.findByCategory_CatmasterIdIn(branch, pageable);
        } else {
            page = productRepository.findByCategory_CatmasterId(catmasterId, pageable);
        }

        boolean cardholder = cardholderService.isCurrentUserCardholder();
        return PageResponse.from(page, p -> productMapper.toSummary(p, cardholder));
    }

    @Override
    public PageResponse<ProductResponse> searchProducts(String search,
                                                        BigDecimal minPrice,
                                                        BigDecimal maxPrice,
                                                        Pageable pageable) {

        if (minPrice != null && minPrice.signum() < 0) {
            throw new BusinessRuleViolationException("minPrice cannot be negative");
        }
        if (maxPrice != null && maxPrice.signum() < 0) {
            throw new BusinessRuleViolationException("maxPrice cannot be negative");
        }
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new BusinessRuleViolationException("minPrice cannot be greater than maxPrice");
        }

        // Blank search box should mean "no filter", not "match the empty string".
        String term = (search == null || search.isBlank()) ? null : search.trim();

        Page<ProductMaster> page = productRepository.search(term, minPrice, maxPrice, pageable);
        boolean cardholder = cardholderService.isCurrentUserCardholder();
        return PageResponse.from(page, p -> productMapper.toSummary(p, cardholder));
    }

    @Override
    public ProductResponse getProduct(Integer prodId) {
        ProductMaster product = productRepository.findByIdWithCategory(prodId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "prodId", prodId));

        List<ProductImage> images =
                productImageRepository.findByProduct_ProdIdOrderByDisplayOrderAsc(prodId);

        return productMapper.toDetail(product, loadVariants(prodId), images,
                cardholderService.isCurrentUserCardholder());
    }

    @Override
    public List<ProductVariantResponse> getProductVariants(Integer prodId) {
        if (!productRepository.existsById(prodId)) {
            throw new ResourceNotFoundException("Product", "prodId", prodId);
        }
        return loadVariants(prodId);
    }

    // ------------------------------------------------------------------

    private List<ProductVariantResponse> loadVariants(Integer prodId) {
        List<ProdDtlMaster> details = prodDtlRepository.findByProductIdWithConfig(prodId);
        return productMapper.groupVariants(details);
    }

    /**
     * Returns this category's id plus every descendant's id.
     *
     * Reads the table once and walks it in memory — the alternative (a query
     * per level) would be N queries deep for a deep tree.
     */
    private List<Integer> collectBranchIds(CategoryMaster root) {

        List<CategoryMaster> all = categoryRepository.findAll();

        Map<String, List<CategoryMaster>> childrenByParentCode = all.stream()
                .filter(c -> c.getSubcatId() != null && !ROOT_MARKER.equals(c.getSubcatId()))
                .collect(Collectors.groupingBy(c -> c.getSubcatId().trim().toUpperCase()));

        Set<Integer> ids = new LinkedHashSet<>();
        Deque<AbstractMap.SimpleEntry<CategoryMaster, Integer>> stack = new ArrayDeque<>();
        stack.push(new AbstractMap.SimpleEntry<>(root, 0));

        while (!stack.isEmpty()) {
            var current = stack.pop();
            CategoryMaster category = current.getKey();
            int depth = current.getValue();

            // add() returns false if we have already seen this id -> cycle guard
            if (!ids.add(category.getCatmasterId()) || depth >= MAX_DEPTH) {
                continue;
            }

            String code = category.getCatId() == null ? null : category.getCatId().trim().toUpperCase();
            if (code == null) continue;

            for (CategoryMaster child : childrenByParentCode.getOrDefault(code, List.of())) {
                stack.push(new AbstractMap.SimpleEntry<>(child, depth + 1));
            }
        }

        log.debug("Category branch for catmasterId={} resolved to {} categories",
                root.getCatmasterId(), ids.size());

        return List.copyOf(ids);
    }
}
