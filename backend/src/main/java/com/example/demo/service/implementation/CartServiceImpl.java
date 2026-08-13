package com.example.demo.service.implementation;

import com.example.demo.dto.request.CartItemRequest;
import com.example.demo.dto.request.UpdateCartItemRequest;
import com.example.demo.dto.response.CartResponse;
import com.example.demo.entity.Cart;
import com.example.demo.entity.CartItem;
import com.example.demo.entity.ProductMaster;
import com.example.demo.entity.User;
import com.example.demo.enums.CartStatus;
import com.example.demo.enums.PriceOption;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.exception.UnauthorizedActionException;
import com.example.demo.mapper.CartMapper;
import com.example.demo.repository.CartItemRepository;
import com.example.demo.repository.CartRepository;
import com.example.demo.repository.ProductMasterRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.SecurityUtils;
import com.example.demo.service.CardholderService;
import com.example.demo.service.PricingService;
import com.example.demo.service.interfaces.CartService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private final CardholderService cardholderService;
    private final PricingService pricingService;

    public CartServiceImpl(CartRepository cartRepository,
                           CartItemRepository cartItemRepository,
                           ProductMasterRepository productRepository,
                           UserRepository userRepository,
                           CartMapper cartMapper,
                           SecurityUtils securityUtils,
                           CardholderService cardholderService,
                           PricingService pricingService) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.cartMapper = cartMapper;
        this.securityUtils = securityUtils;
        this.cardholderService = cardholderService;
        this.pricingService = pricingService;
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

        PriceOption option = request.getPriceOption() == null
                ? PriceOption.REGULAR
                : request.getPriceOption();

        Optional<CartItem> existing =
                cartItemRepository.findByCart_CartIdAndProduct_ProdId(cart.getCartId(), product.getProdId());

        if (existing.isPresent()) {
            // Same product already in the cart -> bump the quantity instead of
            // inserting a duplicate line. The newly chosen option wins, because
            // the shopper just told us what they want by ticking it.
            CartItem item = existing.get();
            int newQuantity = item.getQuantity() + request.getQuantity();

            applyOption(user, item, product, newQuantity, option);
            cartItemRepository.save(item);

            log.debug("Incremented cartItemId={} to quantity={} at {}",
                    item.getCartItemId(), newQuantity, option);

        } else {
            CartItem item = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .build();

            applyOption(user, item, product, request.getQuantity(), option);
            cartItemRepository.save(item);

            log.debug("Added prodId={} to cartId={} at {}",
                    product.getProdId(), cart.getCartId(), option);
        }

        return reload(user);
    }

    @Override
    @Transactional
    public CartResponse updateItem(Integer cartItemId, UpdateCartItemRequest request) {
        User user = currentUser();
        CartItem item = loadOwnedItem(cartItemId, user);

        // A null priceOption means "just changing the quantity" — keep whatever
        // the line already had, so the quantity stepper doesn't have to re-send
        // the pricing choice on every click.
        PriceOption option = request.getPriceOption() == null
                ? item.getPriceOption()
                : request.getPriceOption();

        applyOption(user, item, item.getProduct(), request.getQuantity(), option);
        cartItemRepository.save(item);

        log.debug("Updated cartItemId={} to quantity={} at {}", cartItemId, request.getQuantity(), option);
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

    /**
     * Derived from emart_card.status via CardholderService, NOT from the
     * denormalised users.is_cardholder column - the two used to drift apart.
     */
    private boolean isCardholder(User user) {
        return cardholderService.isActiveCardholder(user.getUserId());
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
     * Validates the chosen option and then writes quantity + option + the
     * resulting points onto the line.
     *
     * pointsUsed is DERIVED here and never read from the request. That is the
     * whole reason the request only carries a price option: a client that sends
     * its own point figure cannot get it honoured.
     *
     * The balance is re-checked again at checkout and at payment, because it
     * can change between adding to the cart and paying.
     */
    private void applyOption(User user, CartItem item, ProductMaster product,
                             int quantity, PriceOption option) {

        boolean cardholder = isCardholder(user);
        int balance = cardholderService.getPointsBalance(user.getUserId());

        pricingService.validate(product, option, quantity, cardholder, balance);

        PricingService.ResolvedPrice price = pricingService.resolve(product, option);

        item.setQuantity(quantity);
        item.setPriceOption(option);
        item.setPointsUsed(price.pointsFor(quantity));
    }

    /** Re-reads the cart with items fetched, so the response is always current. */
    private CartResponse reload(User user) {
        Cart cart = cartRepository.findByUserIdWithItems(user.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Cart", "userId", user.getUserId()));
        return cartMapper.toCartResponse(cart, isCardholder(user));
    }
}
