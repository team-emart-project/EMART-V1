package com.example.demo.service.implementation;

import com.example.demo.dto.request.WishlistRequest;
import com.example.demo.dto.response.WishlistResponse;
import com.example.demo.entity.ProductMaster;
import com.example.demo.entity.User;
import com.example.demo.entity.Wishlist;
import com.example.demo.exception.DuplicateResourceException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.exception.UnauthorizedActionException;
import com.example.demo.mapper.WishlistMapper;
import com.example.demo.repository.ProductMasterRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.WishlistRepository;
import com.example.demo.security.SecurityUtils;
import com.example.demo.service.CardholderService;
import com.example.demo.service.interfaces.WishlistService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class WishlistServiceImpl implements WishlistService {

    private static final Logger log = LoggerFactory.getLogger(WishlistServiceImpl.class);

    private final WishlistRepository wishlistRepository;
    private final ProductMasterRepository productRepository;
    private final UserRepository userRepository;
    private final WishlistMapper wishlistMapper;
    private final SecurityUtils securityUtils;
    private final CardholderService cardholderService;

    public WishlistServiceImpl(WishlistRepository wishlistRepository,
                               ProductMasterRepository productRepository,
                               UserRepository userRepository,
                               WishlistMapper wishlistMapper,
                               SecurityUtils securityUtils,
                               CardholderService cardholderService) {
        this.wishlistRepository = wishlistRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.wishlistMapper = wishlistMapper;
        this.securityUtils = securityUtils;
        this.cardholderService = cardholderService;
    }

    @Override
    public List<WishlistResponse> getMyWishlist() {
        Integer userId = securityUtils.getCurrentUserId();
        boolean cardholder = cardholderService.isActiveCardholder(userId);
        return wishlistRepository.findByUserIdWithProduct(userId).stream()
                .map(w -> wishlistMapper.toResponse(w, cardholder))
                .toList();
    }

    @Override
    @Transactional
    public WishlistResponse addToWishlist(WishlistRequest request) {

        Integer userId = securityUtils.getCurrentUserId();

        ProductMaster product = productRepository.findById(request.getProdId())
                .orElseThrow(() -> new ResourceNotFoundException("Product", "prodId", request.getProdId()));

        // The (user_id, prod_id) UNIQUE constraint would reject this anyway;
        // checking first turns a raw constraint violation into a clear message.
        if (wishlistRepository.existsByUser_UserIdAndProduct_ProdId(userId, product.getProdId())) {
            throw new DuplicateResourceException("This product is already in your wishlist");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "userId", userId));

        Wishlist saved = wishlistRepository.save(Wishlist.builder()
                .user(user)
                .product(product)
                .build());

        log.debug("Added prodId={} to wishlist of userId={}", product.getProdId(), userId);
        return wishlistMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void removeFromWishlist(Integer wishlistId) {

        Wishlist wishlist = wishlistRepository.findById(wishlistId)
                .orElseThrow(() -> new ResourceNotFoundException("Wishlist entry", "wishlistId", wishlistId));

        // Ownership check: without it, changing the id in the URL would let
        // anyone delete from someone else's wishlist.
        if (!wishlist.getUser().getUserId().equals(securityUtils.getCurrentUserId())) {
            throw new UnauthorizedActionException("This wishlist entry does not belong to the current user");
        }

        wishlistRepository.delete(wishlist);
        log.debug("Removed wishlistId={}", wishlistId);
    }
}
