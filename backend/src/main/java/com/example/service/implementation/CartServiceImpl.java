package com.example.service.implementation;

import com.example.dto.request.CartItemRequest;
import com.example.dto.request.UpdateCartItemRequest;
import com.example.dto.response.CartResponse;
import com.example.entity.Cart;
import com.example.entity.CartItem;
import com.example.entity.ProductMaster;
import com.example.entity.User;
import com.example.enums.CartStatus;
import com.example.exception.BusinessRuleViolationException;
import com.example.exception.ResourceNotFoundException;
import com.example.exception.UnauthorizedActionException;
import com.example.mapper.CartMapper;
import com.example.repository.CartItemRepository;
import com.example.repository.CartRepository;
import com.example.repository.ProductMasterRepository;
import com.example.repository.UserRepository;
import com.example.security.SecurityUtils;
import com.example.service.interfaces.CartService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Cart business logic.
 *
 * Class-level @Transactional(readOnly = true) makes reads the default and lets
 * Hibernate skip dirty-checking; the methods that write override it with a
 * plain @Transactional so each one commits or rolls back as a single unit.
 */
@Service
@Transactional(readOnly = true)
public class CartServiceImpl implements CartService {

    private static final Logger log = LoggerFactory.getLogger(CartServiceImpl.class);

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductMasterRepository productRepository;
    private final UserRepository userRepository;
    private final CartMapper cartMapper;
    private final SecurityUtils securityUtils;

    @Value("${emart.cart.require-cardholder-for-points:true}")
    private boolean requireCardholderForPoints;

    public CartServiceImpl(CartRepository cartRepository,
                            CartItemRepository cartItemRepository,
                            ProductMasterRepository productRepository,
                            UserRepository userRepository,
                            CartMapper cartMapper,
                            SecurityUtils securityUtils) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.cartMapper = cartMapper;
        this.securityUtils = securityUtils;
    }

    // ------------------------------------------------------------------
    // READ
    // ------------------------------------------------------------------

    @Override
    public CartResponse getCart() {
        User user = currentUser();
        Cart cart = cartRepository.findByUserIdWithItems(user.getUserId())
                .orElseGet(() -> createCartFor(user));
        return cartMapper.toCartResponse(cart, isCardholder(user));
    }

    // ------------------------------------------------------------------
    // WRITE
    // ------------------------------------------------------------------

    @Override
    @Transactional
    public CartResponse addItem(CartItemRequest request) {
        User user = currentUser();
        Cart cart = getOrCreateCart(user);

        ProductMaster product = productRepository.findById(request.getProdId())
                .orElseThrow(() -> new ResourceNotFoundException("Product", "prodId", request.getProdId()));

        Optional<CartItem> existing =
                cartItemRepository.findByCart_CartIdAndProduct_ProdId(cart.getCartId(), product.getProdId());

        if (existing.isPresent()) {
            // Same product already in the cart -> bump the quantity instead of
            // inserting a duplicate line.
            CartItem item = existing.get();
            int newQuantity = item.getQuantity() + request.getQuantity();
            int newPointsUsed = Boolean.TRUE.equals(request.getRedeemPoints())
                    ? item.getPointsUsed() + request.getPointsUsed()
                    : item.getPointsUsed();
            boolean newRedeemFlag = Boolean.TRUE.equals(item.getRedeemPoints())
                    || Boolean.TRUE.equals(request.getRedeemPoints());

            validatePoints(user, product, newQuantity, newRedeemFlag, newPointsUsed);

            item.setQuantity(newQuantity);
            item.setRedeemPoints(newRedeemFlag);
            item.setPointsUsed(newPointsUsed);
            cartItemRepository.save(item);

            log.debug("Incremented cartItemId={} to quantity={}", item.getCartItemId(), newQuantity);

        } else {
            validatePoints(user, product, request.getQuantity(),
                    request.getRedeemPoints(), request.getPointsUsed());

            CartItem item = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .quantity(request.getQuantity())
                    .redeemPoints(Boolean.TRUE.equals(request.getRedeemPoints()))
                    .pointsUsed(request.getPointsUsed() == null ? 0 : request.getPointsUsed())
                    .build();

            cartItemRepository.save(item);
            log.debug("Added prodId={} to cartId={}", product.getProdId(), cart.getCartId());
        }

        return reload(user);
    }

    @Override
    @Transactional
    public CartResponse updateItem(Integer cartItemId, UpdateCartItemRequest request) {
        User user = currentUser();
        CartItem item = loadOwnedItem(cartItemId, user);

        validatePoints(user, item.getProduct(), request.getQuantity(),
                request.getRedeemPoints(), request.getPointsUsed());

        item.setQuantity(request.getQuantity());
        item.setRedeemPoints(Boolean.TRUE.equals(request.getRedeemPoints()));
        item.setPointsUsed(request.getPointsUsed() == null ? 0 : request.getPointsUsed());
        cartItemRepository.save(item);

        log.debug("Updated cartItemId={} to quantity={}", cartItemId, request.getQuantity());
        return reload(user);
    }

    @Override
    @Transactional
    public CartResponse removeItem(Integer cartItemId) {
        User user = currentUser();
        CartItem item = loadOwnedItem(cartItemId, user);

        cartItemRepository.delete(item);
        log.debug("Removed cartItemId={}", cartItemId);

        return reload(user);
    }

    @Override
    @Transactional
    public CartResponse clearCart() {
        User user = currentUser();
        Cart cart = getOrCreateCart(user);

        // orphanRemoval = true on Cart.items turns this into DELETE statements.
        cart.getItems().clear();
        cartRepository.save(cart);

        log.debug("Cleared cartId={}", cart.getCartId());
        return reload(user);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private User currentUser() {
        Integer userId = securityUtils.getCurrentUserId();
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "userId", userId));
    }

    private boolean isCardholder(User user) {
        return Boolean.TRUE.equals(user.getIsCardholder());
    }

    /**
     * cart.user_id carries a UNIQUE constraint, so each user has exactly one
     * cart row for the lifetime of the account — we create it on first use and
     * reuse it afterwards.
     */
    private Cart getOrCreateCart(User user) {
        return cartRepository.findByUser_UserId(user.getUserId())
                .orElseGet(() -> createCartFor(user));
    }

    private Cart createCartFor(User user) {
        Cart cart = Cart.builder()
                .user(user)
                .status(CartStatus.ACTIVE)
                .build();
        log.debug("Creating first cart for userId={}", user.getUserId());
        return cartRepository.save(cart);
    }

    /**
     * Loads a cart line and proves it belongs to the caller.
     * Without this check, changing the id in the URL would let anyone edit
     * somebody else's cart.
     */
    private CartItem loadOwnedItem(Integer cartItemId, User user) {
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item", "cartItemId", cartItemId));

        if (!item.getCart().getUser().getUserId().equals(user.getUserId())) {
            throw new UnauthorizedActionException("This cart item does not belong to the current user");
        }
        return item;
    }

    /**
     * Points rules:
     *  - redeeming requires an e-MART card (configurable)
     *  - the product must actually be redeemable (points_to_redeem > 0)
     *  - points cannot exceed points_to_redeem * quantity for that line
     *
     * The user's actual POINTS BALANCE is NOT checked here — that happens at
     * checkout (Module 8), because the balance can change between adding to
     * the cart and paying.
     */
    private void validatePoints(User user, ProductMaster product, Integer quantity,
                                Boolean redeemPoints, Integer pointsUsed) {

        boolean redeem = Boolean.TRUE.equals(redeemPoints);
        int points = pointsUsed == null ? 0 : pointsUsed;

        if (!redeem) {
            if (points > 0) {
                throw new BusinessRuleViolationException(
                        "pointsUsed must be 0 when redeemPoints is false");
            }
            return;
        }

        if (requireCardholderForPoints && !isCardholder(user)) {
            throw new BusinessRuleViolationException(
                    "Only e-MART cardholders can redeem e-Points");
        }

        if (product.getPointsToRedeem() == null || product.getPointsToRedeem() <= 0) {
            throw new BusinessRuleViolationException(
                    "Product '%s' is not eligible for e-Points redemption".formatted(product.getProdName()));
        }

        if (points <= 0) {
            throw new BusinessRuleViolationException(
                    "pointsUsed must be greater than 0 when redeemPoints is true");
        }

        int maxAllowed = product.getPointsToRedeem() * quantity;
        if (points > maxAllowed) {
            throw new BusinessRuleViolationException(
                    "pointsUsed (%d) exceeds the maximum %d allowed for %d unit(s) of '%s'"
                            .formatted(points, maxAllowed, quantity, product.getProdName()));
        }
    }

    /** Re-reads the cart with items fetched, so the response is always current. */
    private CartResponse reload(User user) {
        Cart cart = cartRepository.findByUserIdWithItems(user.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Cart", "userId", user.getUserId()));
        return cartMapper.toCartResponse(cart, isCardholder(user));
    }
}
