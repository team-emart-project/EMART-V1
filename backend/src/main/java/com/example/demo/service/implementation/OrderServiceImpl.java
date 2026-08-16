package com.example.demo.service.implementation;

import com.example.demo.dto.request.CheckoutRequest;
import com.example.demo.dto.response.OrderResponse;
import com.example.demo.entity.*;
import com.example.demo.enums.CardStatus;
import com.example.demo.enums.CartStatus;
import com.example.demo.enums.OrderStatus;
import com.example.demo.enums.PaymentStatus;
import com.example.demo.exception.BusinessRuleViolationException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.exception.UnauthorizedActionException;
import com.example.demo.mapper.OrderMapper;
import com.example.demo.repository.*;
import com.example.demo.security.SecurityUtils;
import com.example.demo.enums.PriceOption;
import com.example.demo.service.CardholderService;
import com.example.demo.service.PricingService;
import com.example.demo.service.interfaces.OrderService;
import com.example.demo.util.InvoicePdfGenerator;
import com.example.demo.util.OrderNumberGeneratorUtil;
import com.example.demo.response.PageResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class OrderServiceImpl implements OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);

    private final OrdersRepository ordersRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final AddressRepository addressRepository;
    private final UserRepository userRepository;
    private final EmartCardRepository emartCardRepository;
    private final OrderMapper orderMapper;
    private final OrderNumberGeneratorUtil orderNumberGenerator;
    private final InvoicePdfGenerator invoicePdfGenerator;
    private final SecurityUtils securityUtils;
    private final CardholderService cardholderService;
    private final PricingService pricingService;

    // NO TAX. The customer is charged exactly the price shown on the product
    // card, so there is no tax rate to configure and total == subtotal.

    /**
     * TIERED e-POINTS EARNING (2% - 5% of order value).
     *
     * The rate rises with the order total, so a bigger basket earns
     * proportionally more. Tier boundaries and rates are configurable; the
     * defaults below are the rule this project implements:
     *
     *     under  5,000        ->  2%
     *      5,000 -  24,999    ->  3%
     *     25,000 -  74,999    ->  4%
     *     75,000 and above    ->  5%
     *
     * Points are always rounded DOWN, so we never credit a fraction of a point.
     */
    @Value("${emart.points.earn.tier1-limit:5000}")
    private BigDecimal tier1Limit;
    @Value("${emart.points.earn.tier2-limit:25000}")
    private BigDecimal tier2Limit;
    @Value("${emart.points.earn.tier3-limit:75000}")
    private BigDecimal tier3Limit;

    @Value("${emart.points.earn.tier1-percentage:2}")
    private BigDecimal tier1Pct;
    @Value("${emart.points.earn.tier2-percentage:3}")
    private BigDecimal tier2Pct;
    @Value("${emart.points.earn.tier3-percentage:4}")
    private BigDecimal tier3Pct;
    @Value("${emart.points.earn.tier4-percentage:5}")
    private BigDecimal tier4Pct;

    public OrderServiceImpl(OrdersRepository ordersRepository,
                            CartRepository cartRepository,
                            CartItemRepository cartItemRepository,
                            AddressRepository addressRepository,
                            UserRepository userRepository,
                            EmartCardRepository emartCardRepository,
                            OrderMapper orderMapper,
                            OrderNumberGeneratorUtil orderNumberGenerator,
                            InvoicePdfGenerator invoicePdfGenerator,
                            SecurityUtils securityUtils,
                            CardholderService cardholderService,
                            PricingService pricingService) {
        this.ordersRepository = ordersRepository;
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.addressRepository = addressRepository;
        this.userRepository = userRepository;
        this.emartCardRepository = emartCardRepository;
        this.orderMapper = orderMapper;
        this.orderNumberGenerator = orderNumberGenerator;
        this.invoicePdfGenerator = invoicePdfGenerator;
        this.securityUtils = securityUtils;
        this.cardholderService = cardholderService;
        this.pricingService = pricingService;
    }

    // ------------------------------------------------------------------
    // Preview
    // ------------------------------------------------------------------

    @Override
    public OrderResponse checkoutPreview(CheckoutRequest request) {
        User user = loadCurrentUser();
        Cart cart = loadCartWithItems(user);
        Address shipping = loadOwnedAddress(request.getShippingAddressId(), user);
        Address billing = request.getBillingAddressId() == null
                ? shipping
                : loadOwnedAddress(request.getBillingAddressId(), user);

        Orders draft = buildOrder(user, cart, shipping, billing);

        OrderResponse response = orderMapper.toResponse(draft, null);
        response.setPreview(true);
        // nothing was persisted, so these are meaningless on a preview
        response.setOrderId(null);
        response.setOrderNo(null);
        return response;
    }

    // ------------------------------------------------------------------
    // Place
    // ------------------------------------------------------------------

    @Override
    @Transactional
    public OrderResponse placeOrder(CheckoutRequest request) {

        User user = loadCurrentUser();
        Cart cart = loadCartWithItems(user);
        Address shipping = loadOwnedAddress(request.getShippingAddressId(), user);
        Address billing = request.getBillingAddressId() == null
                ? shipping
                : loadOwnedAddress(request.getBillingAddressId(), user);

        Orders order = buildOrder(user, cart, shipping, billing);
        order.setOrderNo(orderNumberGenerator.generate());
        order.setOrderDate(LocalDateTime.now());

        Orders saved = ordersRepository.save(order);

        // The cart row is REUSED, not replaced: cart.user_id is UNIQUE, so a
        // user cannot hold a CONVERTED cart and a fresh ACTIVE one at the same
        // time. Emptying it leaves the same row ready for the next shop.
        cartItemRepository.deleteAll(cart.getItems());
        cart.getItems().clear();
        cart.setStatus(CartStatus.ACTIVE);
        cartRepository.save(cart);

        log.info("Placed orderNo={} for userId={} total={}",
                saved.getOrderNo(), user.getUserId(), saved.getTotalAmount());

        return orderMapper.toResponse(saved, null);
    }

    // ------------------------------------------------------------------
    // Read
    // ------------------------------------------------------------------

    @Override
    public PageResponse<OrderResponse> getMyOrders(Pageable pageable) {
        Integer userId = securityUtils.getCurrentUserId();
        Page<Orders> page = ordersRepository.findByUser_UserIdOrderByOrderDateDesc(userId, pageable);
        return PageResponse.from(page,
                o -> orderMapper.toResponse(o, null));
    }

    @Override
    public OrderResponse getOrder(Integer orderId) {
        Orders order = loadOwnedOrder(orderId);
        return orderMapper.toResponse(order, null);
    }

    // ------------------------------------------------------------------
    // Cancel
    // ------------------------------------------------------------------

    @Override
    @Transactional
    public OrderResponse cancelOrder(Integer orderId) {

        Orders order = loadOwnedOrder(orderId);

        // State machine enforced on the SERVER. Never trust the client to know
        // whether cancelling is allowed.
        if (order.getOrderStatus() == OrderStatus.CANCELLED) {
            throw new BusinessRuleViolationException("This order is already cancelled");
        }
        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            throw new BusinessRuleViolationException(
                    "A paid order cannot be cancelled here. Please raise a refund request.");
        }
        if (order.getOrderStatus() != OrderStatus.PLACED) {
            throw new BusinessRuleViolationException(
                    "Only an order still in PLACED status can be cancelled");
        }

        order.setOrderStatus(OrderStatus.CANCELLED);
        Orders saved = ordersRepository.save(order);
        log.info("Cancelled orderNo={}", saved.getOrderNo());

        return orderMapper.toResponse(saved, null);
    }

    // ------------------------------------------------------------------
    // Invoice
    // ------------------------------------------------------------------

    @Override
    public byte[] generateInvoicePdf(Integer orderId) {
        Orders order = loadOwnedOrder(orderId);

        if (order.getPaymentStatus() != PaymentStatus.PAID) {
            throw new BusinessRuleViolationException(
                    "An invoice is only available once the order has been paid");
        }
        return invoicePdfGenerator.generate(order);
    }

    // ==================================================================
    // Calculation
    // ==================================================================

    /**
     * Builds an unsaved Orders graph from the cart. Used by BOTH preview and
     * place-order, so the numbers a customer is shown are computed by exactly
     * the same code that later persists them.
     */
    private Orders buildOrder(User user, Cart cart, Address shipping, Address billing) {

        // Derived from emart_card.status, not the denormalised users.is_cardholder
        // flag - the two used to drift apart.
        boolean cardholder = cardholderService.isActiveCardholder(user.getUserId());

        Orders order = Orders.builder()
                .user(user)
                .shippingAddress(shipping)
                .billingAddress(billing)
                .orderDate(LocalDateTime.now())
                .paymentStatus(PaymentStatus.PENDING)
                .orderStatus(OrderStatus.PLACED)
                .items(new ArrayList<>())
                .build();

        int pointsAvailable = currentPointsBalance(user);

        BigDecimal subtotal = BigDecimal.ZERO;
        int totalPointsRedeemed = 0;

        for (CartItem cartItem : cart.getItems()) {
            ProductMaster product = cartItem.getProduct();
            int quantity = cartItem.getQuantity();
            PriceOption option = cartItem.getPriceOption() == null
                    ? PriceOption.REGULAR
                    : cartItem.getPriceOption();

            // Re-validated and re-priced from the LIVE product row, not from
            // whatever was stored when the item went into the cart. A price or
            // an offer can change while a cart sits open, and the shopper must
            // be charged today's rules, not last week's.
            pricingService.validate(product, option, quantity, cardholder, pointsAvailable);
            PricingService.ResolvedPrice price = pricingService.resolve(product, option);

            subtotal = subtotal.add(price.cashFor(quantity));
            totalPointsRedeemed += price.pointsFor(quantity);

            order.addItem(OrderDetail.builder()
                    .product(product)
                    // Snapshot the NAME so an old invoice never changes if the
                    // catalog is edited later.
                    .prodNameSnapshot(product.getProdName())
                    .quantity(quantity)
                    .mrpPrice(product.getMrpPrice())
                    .cardholderPrice(product.getCardholderPrice())
                    .priceOption(option)
                    .priceCharged(price.cashPerUnit())
                    .pointsRedeemed(price.pointsFor(quantity))
                    .build());
        }

        subtotal = scale(subtotal);

        // Whole-basket points check. Each line was validated on its own above,
        // but three affordable lines can still add up to more points than the
        // shopper has.
        if (totalPointsRedeemed > pointsAvailable) {
            throw new BusinessRuleViolationException(
                    "This order needs %d e-Points but you only have %d"
                            .formatted(totalPointsRedeemed, pointsAvailable));
        }

        // NO TAX and no points "discount": the option a shopper picked already
        // decided the cash price for that line, so the total is simply the sum.
        BigDecimal total = subtotal;

        // Only cardholders earn points, and only on the cash they actually spend.
        int pointsEarned = cardholder ? calculatePointsEarned(total) : 0;

        order.setSubtotalAmount(subtotal);
        order.setTotalAmount(total);
        order.setPointsRedeemed(totalPointsRedeemed);
        order.setPointsEarned(pointsEarned);

        return order;
    }

    private BigDecimal scale(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private int currentPointsBalance(User user) {
        // Single source of truth - see CardholderService for why this matters.
        return cardholderService.getPointsBalance(user.getUserId());
    }

    /**
     * Applies the tiered earn rate to the order total.
     * Package-private so the unit test can exercise the boundaries directly.
     */
    int calculatePointsEarned(BigDecimal orderTotal) {
        if (orderTotal == null || orderTotal.signum() <= 0) return 0;

        BigDecimal rate;
        if (orderTotal.compareTo(tier1Limit) < 0)        rate = tier1Pct;   // 2%
        else if (orderTotal.compareTo(tier2Limit) < 0)   rate = tier2Pct;   // 3%
        else if (orderTotal.compareTo(tier3Limit) < 0)   rate = tier3Pct;   // 4%
        else                                             rate = tier4Pct;   // 5%

        return orderTotal.multiply(rate)
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.DOWN)
                .intValue();
    }

    // ==================================================================
    // Loading + ownership
    // ==================================================================

    private User loadCurrentUser() {
        Integer userId = securityUtils.getCurrentUserId();
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "userId", userId));
    }

    private Cart loadCartWithItems(User user) {
        Cart cart = cartRepository.findByUserIdWithItems(user.getUserId())
                .orElseThrow(() -> new BusinessRuleViolationException(
                        "Your cart is empty. Add something before checking out."));

        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new BusinessRuleViolationException(
                    "Your cart is empty. Add something before checking out.");
        }
        return cart;
    }

    private Address loadOwnedAddress(Integer addressId, User user) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address", "addressId", addressId));

        if (!address.getUser().getUserId().equals(user.getUserId())) {
            throw new UnauthorizedActionException("That address does not belong to you");
        }
        return address;
    }

    private Orders loadOwnedOrder(Integer orderId) {
        Orders order = ordersRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "orderId", orderId));

        if (!order.getUser().getUserId().equals(securityUtils.getCurrentUserId())) {
            throw new UnauthorizedActionException("That order does not belong to you");
        }
        return order;
    }
}
