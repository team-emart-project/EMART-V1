package com.example.demo.service;

import com.example.demo.dto.request.CartItemRequest;
import com.example.demo.dto.request.UpdateCartItemRequest;
import com.example.demo.dto.response.CartResponse;
import com.example.demo.entity.Cart;
import com.example.demo.entity.CartItem;
import com.example.demo.entity.ProductMaster;
import com.example.demo.entity.User;
import com.example.demo.enums.CartStatus;
import com.example.demo.enums.PriceOption;
import com.example.demo.exception.BusinessRuleViolationException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.exception.UnauthorizedActionException;
import com.example.demo.mapper.CartMapper;
import com.example.demo.repository.CartItemRepository;
import com.example.demo.repository.CartRepository;
import com.example.demo.repository.ProductMasterRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.SecurityUtils;
import com.example.demo.service.implementation.CartServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for Module 6 cart logic, under the four-price-option model.
 *
 * Repositories are mocked, so nothing touches MySQL — these run in
 * milliseconds and test the RULES, not the database. PricingService and
 * CartMapper are REAL objects, because the prices and totals they compute are
 * precisely what we want to verify; mocking them would test nothing.
 */
@ExtendWith(MockitoExtension.class)
class CartServiceImplTest {

    @Mock private CartRepository cartRepository;
    @Mock private CartItemRepository cartItemRepository;
    @Mock private ProductMasterRepository productRepository;
    @Mock private UserRepository userRepository;
    @Mock private SecurityUtils securityUtils;
    @Mock private CardholderService cardholderService;

    private PricingService pricingService;
    private CartMapper cartMapper;

    @InjectMocks private CartServiceImpl cartService;

    private User cardholder;
    private User normalUser;

    /** Carries all three offers: member 1100, points 1100, hybrid 650 + 450. */
    private ProductMaster fullOfferProduct;

    /** Carries no member offer at all — every checkbox should be unavailable. */
    private ProductMaster plainProduct;

    private Cart cart;

    @BeforeEach
    void setUp() {
        pricingService = new PricingService();
        ReflectionTestUtils.setField(pricingService, "requireCardholder", true);
        cartMapper = new CartMapper(pricingService);

        // @InjectMocks only wires @Mock fields, so the two real collaborators
        // are pushed in by hand.
        ReflectionTestUtils.setField(cartService, "pricingService", pricingService);
        ReflectionTestUtils.setField(cartService, "cartMapper", cartMapper);

        cardholder = User.builder()
                .userId(1).email("rishi@example.com").isCardholder(true).isActive(true)
                .build();

        normalUser = User.builder()
                .userId(2).email("ananya@example.com").isCardholder(false).isActive(true)
                .build();

        fullOfferProduct = ProductMaster.builder()
                .prodId(10)
                .prodName("Lava A1 Josh")
                .mrpPrice(new BigDecimal("1300.00"))
                .cardholderPrice(new BigDecimal("1100.00"))
                .pointsPrice(1100)
                .hybridCashPrice(new BigDecimal("650.00"))
                .hybridPoints(450)
                .build();

        plainProduct = ProductMaster.builder()
                .prodId(11)
                .prodName("Atomic Habits")
                .mrpPrice(new BigDecimal("500.00"))
                .build();

        cart = Cart.builder()
                .cartId(100).user(cardholder).status(CartStatus.ACTIVE)
                .items(new ArrayList<>())
                .build();
    }

    // ---------------------------------------------------------------
    // Pricing per option
    // ---------------------------------------------------------------

    @Test
    @DisplayName("REGULAR charges the normal price even for a cardholder")
    void regularChargesNormalPrice() {
        // A cardholder who ticks nothing pays the normal price. Membership is
        // an option they choose, not something applied behind their back.
        cart.getItems().add(cartItem(1, fullOfferProduct, 2, PriceOption.REGULAR, 0));
        stubCardholderRead();

        CartResponse response = cartService.getCart();

        assertThat(response.getSubtotalPayable()).isEqualByComparingTo("2600.00"); // 1300 x 2
        assertThat(response.getTotalSavings()).isEqualByComparingTo("0.00");
        assertThat(response.getTotalPointsUsed()).isZero();
    }

    @Test
    @DisplayName("MEMBER charges the member price and reports the saving")
    void memberChargesMemberPrice() {
        cart.getItems().add(cartItem(1, fullOfferProduct, 2, PriceOption.MEMBER, 0));
        stubCardholderRead();

        CartResponse response = cartService.getCart();

        assertThat(response.getSubtotalPayable()).isEqualByComparingTo("2200.00"); // 1100 x 2
        assertThat(response.getSubtotalMrp()).isEqualByComparingTo("2600.00");
        assertThat(response.getTotalSavings()).isEqualByComparingTo("400.00");
        assertThat(response.getTotalPointsUsed()).isZero();
    }

    @Test
    @DisplayName("POINTS charges zero cash and spends the full points price")
    void pointsChargesNoCash() {
        cart.getItems().add(cartItem(1, fullOfferProduct, 2, PriceOption.POINTS, 2200));
        stubCardholderRead();

        CartResponse response = cartService.getCart();

        assertThat(response.getSubtotalPayable()).isEqualByComparingTo("0.00");
        assertThat(response.getTotalPointsUsed()).isEqualTo(2200); // 1100 x 2
        assertThat(response.getItems().get(0).getUnitPriceApplied()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("HYBRID charges the cash half and spends the points half")
    void hybridSplitsCashAndPoints() {
        cart.getItems().add(cartItem(1, fullOfferProduct, 2, PriceOption.HYBRID, 900));
        stubCardholderRead();

        CartResponse response = cartService.getCart();

        assertThat(response.getSubtotalPayable()).isEqualByComparingTo("1300.00"); // 650 x 2
        assertThat(response.getTotalPointsUsed()).isEqualTo(900);                  // 450 x 2
    }

    @Test
    @DisplayName("a non-cardholder never sees the member price in the payload")
    void memberPriceHiddenFromNonCardholder() {
        Cart normalCart = Cart.builder()
                .cartId(200).user(normalUser).status(CartStatus.ACTIVE)
                .items(new ArrayList<>()).build();
        CartItem item = cartItem(1, fullOfferProduct, 1, PriceOption.REGULAR, 0);
        item.setCart(normalCart);
        normalCart.getItems().add(item);

        when(securityUtils.getCurrentUserId()).thenReturn(2);
        when(userRepository.findById(2)).thenReturn(Optional.of(normalUser));
        when(cartRepository.findByUserIdWithItems(2)).thenReturn(Optional.of(normalCart));
        when(cardholderService.isActiveCardholder(2)).thenReturn(false);

        CartResponse response = cartService.getCart();

        assertThat(response.getSubtotalPayable()).isEqualByComparingTo("1300.00");
        // null, so @JsonInclude(NON_NULL) drops it from the JSON entirely
        assertThat(response.getItems().get(0).getCardholderPrice()).isNull();
    }

    // ---------------------------------------------------------------
    // Add / merge behaviour
    // ---------------------------------------------------------------

    @Test
    @DisplayName("adding a product already in the cart increments quantity, not a new row")
    void addingDuplicateIncrementsQuantity() {
        CartItem existing = cartItem(1, fullOfferProduct, 2, PriceOption.REGULAR, 0);
        cart.getItems().add(existing);
        stubCardholderWrite();

        when(cartRepository.findByUser_UserId(1)).thenReturn(Optional.of(cart));
        when(productRepository.findById(10)).thenReturn(Optional.of(fullOfferProduct));
        when(cartItemRepository.findByCart_CartIdAndProduct_ProdId(100, 10))
                .thenReturn(Optional.of(existing));
        when(cartRepository.findByUserIdWithItems(1)).thenReturn(Optional.of(cart));

        cartService.addItem(CartItemRequest.builder()
                .prodId(10).quantity(3).priceOption(PriceOption.REGULAR).build());

        ArgumentCaptor<CartItem> captor = ArgumentCaptor.forClass(CartItem.class);
        verify(cartItemRepository).save(captor.capture());

        assertThat(captor.getValue().getQuantity()).isEqualTo(5); // 2 + 3
    }

    @Test
    @DisplayName("the newly ticked option replaces the one already on the line")
    void newOptionWinsOnMerge() {
        CartItem existing = cartItem(1, fullOfferProduct, 1, PriceOption.REGULAR, 0);
        cart.getItems().add(existing);
        stubCardholderWrite();

        when(cartRepository.findByUser_UserId(1)).thenReturn(Optional.of(cart));
        when(productRepository.findById(10)).thenReturn(Optional.of(fullOfferProduct));
        when(cartItemRepository.findByCart_CartIdAndProduct_ProdId(100, 10))
                .thenReturn(Optional.of(existing));
        when(cartRepository.findByUserIdWithItems(1)).thenReturn(Optional.of(cart));

        cartService.addItem(CartItemRequest.builder()
                .prodId(10).quantity(1).priceOption(PriceOption.MEMBER).build());

        assertThat(existing.getPriceOption()).isEqualTo(PriceOption.MEMBER);
    }

    @Test
    @DisplayName("omitting priceOption defaults to REGULAR, never to spending points")
    void omittedOptionDefaultsToRegular() {
        stubCardholderWrite();
        when(cartRepository.findByUser_UserId(1)).thenReturn(Optional.of(cart));
        when(productRepository.findById(10)).thenReturn(Optional.of(fullOfferProduct));
        when(cartItemRepository.findByCart_CartIdAndProduct_ProdId(100, 10))
                .thenReturn(Optional.empty());
        when(cartRepository.findByUserIdWithItems(1)).thenReturn(Optional.of(cart));

        CartItemRequest request = new CartItemRequest();
        request.setProdId(10);
        request.setQuantity(1);
        request.setPriceOption(null);

        cartService.addItem(request);

        ArgumentCaptor<CartItem> captor = ArgumentCaptor.forClass(CartItem.class);
        verify(cartItemRepository).save(captor.capture());
        assertThat(captor.getValue().getPriceOption()).isEqualTo(PriceOption.REGULAR);
        assertThat(captor.getValue().getPointsUsed()).isZero();
    }

    @Test
    @DisplayName("adding an unknown product returns 404-style ResourceNotFoundException")
    void addingUnknownProductFails() {
        when(securityUtils.getCurrentUserId()).thenReturn(1);
        when(userRepository.findById(1)).thenReturn(Optional.of(cardholder));
        when(cartRepository.findByUser_UserId(1)).thenReturn(Optional.of(cart));
        when(productRepository.findById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.addItem(
                CartItemRequest.builder().prodId(999).quantity(1).build()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Product");
    }

    @Test
    @DisplayName("a user with no cart yet gets one created automatically")
    void cartIsCreatedOnFirstUse() {
        when(securityUtils.getCurrentUserId()).thenReturn(1);
        when(userRepository.findById(1)).thenReturn(Optional.of(cardholder));
        when(cardholderService.isActiveCardholder(1)).thenReturn(true);
        when(cartRepository.findByUserIdWithItems(1)).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        CartResponse response = cartService.getCart();

        verify(cartRepository).save(any(Cart.class));
        assertThat(response.getStatus()).isEqualTo("ACTIVE");
    }

    // ---------------------------------------------------------------
    // Option availability + affordability
    // ---------------------------------------------------------------

    @Test
    @DisplayName("a non-cardholder cannot use any member option")
    void nonCardholderCannotUseMemberOption() {
        Cart normalCart = Cart.builder()
                .cartId(200).user(normalUser).status(CartStatus.ACTIVE)
                .items(new ArrayList<>()).build();

        when(securityUtils.getCurrentUserId()).thenReturn(2);
        when(userRepository.findById(2)).thenReturn(Optional.of(normalUser));
        when(cardholderService.isActiveCardholder(2)).thenReturn(false);
        when(cartRepository.findByUser_UserId(2)).thenReturn(Optional.of(normalCart));
        when(productRepository.findById(10)).thenReturn(Optional.of(fullOfferProduct));
        when(cartItemRepository.findByCart_CartIdAndProduct_ProdId(200, 10))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.addItem(CartItemRequest.builder()
                .prodId(10).quantity(1).priceOption(PriceOption.MEMBER).build()))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("cardholders");
    }

    @Test
    @DisplayName("an option the product does not offer is rejected, not silently downgraded")
    void unofferedOptionRejected() {
        stubCardholderWrite();
        when(cartRepository.findByUser_UserId(1)).thenReturn(Optional.of(cart));
        when(productRepository.findById(11)).thenReturn(Optional.of(plainProduct));
        when(cartItemRepository.findByCart_CartIdAndProduct_ProdId(100, 11))
                .thenReturn(Optional.empty());

        // Charging the normal price instead would bill a different number than
        // the shopper was shown, so this must fail loudly.
        assertThatThrownBy(() -> cartService.addItem(CartItemRequest.builder()
                .prodId(11).quantity(1).priceOption(PriceOption.POINTS).build()))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("not available");
    }

    @Test
    @DisplayName("a points purchase beyond the balance is rejected with the numbers")
    void notEnoughPointsRejected() {
        when(securityUtils.getCurrentUserId()).thenReturn(1);
        when(userRepository.findById(1)).thenReturn(Optional.of(cardholder));
        when(cardholderService.isActiveCardholder(1)).thenReturn(true);
        when(cardholderService.getPointsBalance(1)).thenReturn(250);   // arjun-sized balance
        when(cartRepository.findByUser_UserId(1)).thenReturn(Optional.of(cart));
        when(productRepository.findById(10)).thenReturn(Optional.of(fullOfferProduct));
        when(cartItemRepository.findByCart_CartIdAndProduct_ProdId(100, 10))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.addItem(CartItemRequest.builder()
                .prodId(10).quantity(1).priceOption(PriceOption.POINTS).build()))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("1100")     // needed
                .hasMessageContaining("250");     // held
    }

    @Test
    @DisplayName("points spent are derived from the catalogue, never from the request")
    void pointsAreDerivedNotSupplied() {
        stubCardholderWrite();
        when(cartRepository.findByUser_UserId(1)).thenReturn(Optional.of(cart));
        when(productRepository.findById(10)).thenReturn(Optional.of(fullOfferProduct));
        when(cartItemRepository.findByCart_CartIdAndProduct_ProdId(100, 10))
                .thenReturn(Optional.empty());
        when(cartRepository.findByUserIdWithItems(1)).thenReturn(Optional.of(cart));

        cartService.addItem(CartItemRequest.builder()
                .prodId(10).quantity(2).priceOption(PriceOption.HYBRID).build());

        ArgumentCaptor<CartItem> captor = ArgumentCaptor.forClass(CartItem.class);
        verify(cartItemRepository).save(captor.capture());
        assertThat(captor.getValue().getPointsUsed()).isEqualTo(900); // 450 x 2, from the product
    }

    // ---------------------------------------------------------------
    // Update
    // ---------------------------------------------------------------

    @Test
    @DisplayName("updating quantity alone keeps the line's existing price option")
    void updateKeepsExistingOption() {
        CartItem item = cartItem(1, fullOfferProduct, 1, PriceOption.MEMBER, 0);
        cart.getItems().add(item);
        stubCardholderWrite();

        when(cartItemRepository.findById(1)).thenReturn(Optional.of(item));
        when(cartRepository.findByUserIdWithItems(1)).thenReturn(Optional.of(cart));

        cartService.updateItem(1, UpdateCartItemRequest.builder().quantity(4).build());

        assertThat(item.getPriceOption()).isEqualTo(PriceOption.MEMBER);
        assertThat(item.getQuantity()).isEqualTo(4);
    }

    // ---------------------------------------------------------------
    // Ownership
    // ---------------------------------------------------------------

    @Test
    @DisplayName("a user cannot update a cart item belonging to somebody else")
    void cannotUpdateAnotherUsersItem() {
        Cart otherCart = Cart.builder()
                .cartId(300).user(normalUser).status(CartStatus.ACTIVE)
                .items(new ArrayList<>()).build();
        CartItem foreignItem = cartItem(55, fullOfferProduct, 1, PriceOption.REGULAR, 0);
        foreignItem.setCart(otherCart);

        when(securityUtils.getCurrentUserId()).thenReturn(1);
        when(userRepository.findById(1)).thenReturn(Optional.of(cardholder));
        when(cartItemRepository.findById(55)).thenReturn(Optional.of(foreignItem));

        assertThatThrownBy(() -> cartService.updateItem(55,
                UpdateCartItemRequest.builder().quantity(3).build()))
                .isInstanceOf(UnauthorizedActionException.class);

        verify(cartItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("a user cannot delete a cart item belonging to somebody else")
    void cannotDeleteAnotherUsersItem() {
        Cart otherCart = Cart.builder()
                .cartId(300).user(normalUser).status(CartStatus.ACTIVE)
                .items(new ArrayList<>()).build();
        CartItem foreignItem = cartItem(56, fullOfferProduct, 1, PriceOption.REGULAR, 0);
        foreignItem.setCart(otherCart);

        when(securityUtils.getCurrentUserId()).thenReturn(1);
        when(userRepository.findById(1)).thenReturn(Optional.of(cardholder));
        when(cartItemRepository.findById(56)).thenReturn(Optional.of(foreignItem));

        assertThatThrownBy(() -> cartService.removeItem(56))
                .isInstanceOf(UnauthorizedActionException.class);

        verify(cartItemRepository, never()).delete(any());
    }

    // ---------------------------------------------------------------
    // Clear
    // ---------------------------------------------------------------

    @Test
    @DisplayName("clearing the cart removes every line")
    void clearCartEmptiesItems() {
        cart.getItems().add(cartItem(1, fullOfferProduct, 2, PriceOption.REGULAR, 0));

        when(securityUtils.getCurrentUserId()).thenReturn(1);
        when(userRepository.findById(1)).thenReturn(Optional.of(cardholder));
        when(cardholderService.isActiveCardholder(1)).thenReturn(true);
        when(cartRepository.findByUser_UserId(1)).thenReturn(Optional.of(cart));
        when(cartRepository.findByUserIdWithItems(1)).thenReturn(Optional.of(cart));

        CartResponse response = cartService.clearCart();

        assertThat(cart.getItems()).isEmpty();
        assertThat(response.getTotalQuantity()).isZero();
        assertThat(response.getSubtotalPayable()).isEqualByComparingTo("0");
        verify(cartRepository).save(cart);
    }

    // ---------------------------------------------------------------

    /** Stubs for a read-only path: identity + cardholder status only. */
    private void stubCardholderRead() {
        when(securityUtils.getCurrentUserId()).thenReturn(1);
        when(userRepository.findById(1)).thenReturn(Optional.of(cardholder));
        when(cartRepository.findByUserIdWithItems(1)).thenReturn(Optional.of(cart));
        when(cardholderService.isActiveCardholder(1)).thenReturn(true);
    }

    /** Stubs for a write path: adds a healthy points balance. */
    private void stubCardholderWrite() {
        when(securityUtils.getCurrentUserId()).thenReturn(1);
        when(userRepository.findById(1)).thenReturn(Optional.of(cardholder));
        when(cardholderService.isActiveCardholder(1)).thenReturn(true);
        when(cardholderService.getPointsBalance(1)).thenReturn(50_000);
    }

    private CartItem cartItem(Integer id, ProductMaster product, int qty,
                              PriceOption option, int points) {
        return CartItem.builder()
                .cartItemId(id)
                .cart(cart)
                .product(product)
                .quantity(qty)
                .priceOption(option)
                .pointsUsed(points)
                .build();
    }
}
