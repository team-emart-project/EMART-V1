package com.example.demo.service;

import com.example.demo.dto.response.PageResponse;
import com.example.demo.dto.response.ProductResponse;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.entity.Product;
import com.example.demo.repository.ProductRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    // GET /api/products -> plain paginated list
    public PageResponse<ProductResponse> getAllProducts(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ProductResponse> result = productRepository.findAll(pageable).map(ProductResponse::from);
        return PageResponse.from(result);
    }

    // GET /api/products/{id}
    public ProductResponse getProductById(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));
        return ProductResponse.from(product);
    }

    // GET /api/products/filter?brand=&price=&catmasterId=
    // 'price' is treated as a "max price" ceiling (mrp_price <= price), matching the
    // BRD's "price range" drop-down filter. Adjust to minPrice/maxPrice if you'd rather
    // support a true range.
    public PageResponse<ProductResponse> filterProducts(String brand, BigDecimal price,
                                                          Integer catmasterId, int page, int size) {
        Specification<Product> spec = buildFilterSpec(brand, price, catmasterId, null);
        Pageable pageable = PageRequest.of(page, size);
        Page<ProductResponse> result = productRepository.findAll(spec, pageable).map(ProductResponse::from);
        return PageResponse.from(result);
    }

    // GET /api/products/search?q=
    public PageResponse<ProductResponse> searchProducts(String query, int page, int size) {
        Specification<Product> spec = buildFilterSpec(null, null, null, query);
        Pageable pageable = PageRequest.of(page, size);
        Page<ProductResponse> result = productRepository.findAll(spec, pageable).map(ProductResponse::from);
        return PageResponse.from(result);
    }

    private Specification<Product> buildFilterSpec(String brand, BigDecimal maxPrice,
                                                     Integer catmasterId, String searchTerm) {
        return (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (brand != null && !brand.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("brand")), brand.toLowerCase()));
            }
            if (maxPrice != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("mrpPrice"), maxPrice));
            }
            if (catmasterId != null) {
                predicates.add(cb.equal(root.get("category").get("catmasterId"), catmasterId));
            }
            if (searchTerm != null && !searchTerm.isBlank()) {
                String like = "%" + searchTerm.toLowerCase() + "%";
                Predicate nameMatch = cb.like(cb.lower(root.get("productName")), like);
                Predicate shortDescMatch = cb.like(cb.lower(root.get("productShortDesc")), like);
                predicates.add(cb.or(nameMatch, shortDescMatch));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
