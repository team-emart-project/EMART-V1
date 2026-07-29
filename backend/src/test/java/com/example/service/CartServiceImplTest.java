package com.example.service;

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
import com.example.service.implementation.CartServiceImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for Module 6 cart logic.
 *
 * Repositories are mocked, so nothing touches MySQL — these run in milliseconds
 * and test the RULES, not the database. CartMapper is a @Spy (the real object)
 * because the totals it computes are part of what we want to verify.
 */
@ExtendWith(MockitoExtension.class)
class CartServiceImplTest {

    @Mock private CartRepository cartRepository;
    @Mock private CartItemRepository cartItemRepository;
    @Mock private ProductMasterRepository productRepository;
    @Mock private UserRepository userRepository;
    @Mock private SecurityUtils securityUtils;
    @Spy  private CartMapper cartMapper = new CartMapper();

    @InjectMocks private CartServiceImpl cartService;

    private User cardholder;
    private User normalUser;
    private ProductMaster redeemableProduct;
    private Cart cart;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(cartService, "requireCardholderForPoints", true);

        cardholder = User.builder()
                .userId(1).email("rishi@example.com").isCardholder(true).isActive(true)
                .build();

        normalUser = User.builder()
                .userId(2).email("ananya@example.com").isCardholder(false).isActive(true)
                .build();

        redeemableProduct = ProductMaster.builder()
                .prodId(10)
                .prodName("Canon EOS 1500D DSLR Camera")
                .mrpPrice(new BigDecimal("32999.00"))
                .cardholderPrice(new BigDecimal("29999.00"))
                .pointsToRedeem(500)
                .build();

        cart = Cart.builder()
                .cartId(100).user(cardholder).status(CartStatus.ACTIVE)
                .items(new ArrayList<>())
                .build();
    }

    // ---------------------------------------------------------------
    // Pricing
    // ---------------------------------------------------------------

    @Test
    @DisplayName("cardholder is charged cardholder_price and savings are reported")
    void cardholderGetsCardholderPrice() {
        CartItem item = cartItem(1, redeemableProduct, 2, false, 0);
        cart.getItems().add(item);

        when(securityUtils.getCurrentUserId()).thenReturn(1);
        when(userRepository.findById(1)).thenReturn(Optional.of(cardholder));
        when(cartRepository.findByUserIdWithItems(1)).thenReturn(Optional.of(cart));

        CartResponse response = cartService.getCart();

        assertThat(response.getCardholder()).isTrue();
        assertThat(response.getSubtotalPayable()).isEqualByComparingTo("59998.00"); // 29999 * 2
        assertThat(response.getSubtotalMrp()).isEqualByComparingTo("65998.00");     // 32999 * 2
        assertThat(response.getTotalSavings()).isEqualByComparingTo("6000.00");
        assertThat(response.getTotalQuantity()).isEqualTo(2);
        assertThat(response.getDistinctItemCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("non-cardholder is charged mrp_price and saves nothing")
    void normalUserGetsMrpPrice() {
        Cart normalCart = Cart.builder()
                .cartId(200).user(normalUser).status(CartStatus.ACTIVE)
                .items(new ArrayList<>()).build();
        normalCart.getItems().add(cartItem(1, redeemableProduct, 1, false, 0));

        when(securityUtils.getCurrentUserId()).thenReturn(2);
        when(userRepository.findById(2)).thenReturn(Optional.of(normalUser));
        when(cartRepository.findByUserIdWithItems(2)).thenReturn(Optional.of(normalCart));

        CartResponse response = cartService.getCart();

        assertThat(response.getSubtotalPayable()).isEqualByComparingTo("32999.00");
        assertThat(response.getTotalSavings()).isEqualByComparingTo("0.00");
    }

    // ---------------------------------------------------------------
    // Add / merge behaviour
    // ---------------------------------------------------------------

    @Test
    @DisplayName("adding a product already in the cart increments quantity, not a new row")
    void addingDuplicateIncrementsQuantity() {
        CartItem existing = cartItem(1, redeemableProduct, 2, false, 0);
        cart.getItems().add(existing);

        when(securityUtils.getCurrentUserId()).thenReturn(1);
        when(userRepository.findById(1)).thenReturn(Optional.of(cardholder));
        when(cartRepository.findByUser_UserId(1)).thenReturn(Optional.of(cart));
        when(productRepository.findById(10)).thenReturn(Optional.of(redeemableProduct));
        when(cartItemRepository.findByCart_CartIdAndProduct_ProdId(100, 10))
                .thenReturn(Optional.of(existing));
        when(cartRepository.findByUserIdWithItems(1)).thenReturn(Optional.of(cart));

        cartService.addItem(CartItemRequest.builder()
                .prodId(10).quantity(3).redeemPoints(false).pointsUsed(0).build());

        ArgumentCaptor<CartItem> captor = ArgumentCaptor.forClass(CartItem.class);
        verify(cartItemRepository).save(captor.capture());

        assertThat(captor.getValue().getQuantity()).isEqualTo(5); // 2 + 3
        verify(cartItemRepository, never()).save(argThat(i -> i.getCartItemId() == null));
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
        when(cartRepository.findByUserIdWithItems(1)).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        CartResponse response = cartService.getCart();

        verify(cartRepository).save(any(Cart.class));
        assertThat(response.getStatus()).isEqualTo("ACTIVE");
    }

    // ---------------------------------------------------------------
    // e-Points rules
    // ---------------------------------------------------------------

    @Test
    @DisplayName("non-cardholder cannot redeem e-Points")
    void nonCardholderCannotRedeemPoints() {
        Cart normalCart = Cart.builder()
                .cartId(200).user(normalUser).status(CartStatus.ACTIVE)
                .items(new ArrayList<>()).build();

        when(securityUtils.getCurrentUserId()).thenReturn(2);
        when(userRepository.findById(2)).thenReturn(Optional.of(normalUser));
        when(cartRepository.findByUser_UserId(2)).thenReturn(Optional.of(normalCart));
        when(productRepository.findById(10)).thenReturn(Optional.of(redeemableProduct));
        when(cartItemRepository.findByCart_CartIdAndProduct_ProdId(200, 10))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.addItem(CartItemRequest.builder()
                .prodId(10).quantity(1).redeemPoints(true).pointsUsed(100).build()))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("cardholders");
    }

    @Test
    @DisplayName("points above points_to_redeem * quantity are rejected")
    void tooManyPointsRejected() {
        when(securityUtils.getCurrentUserId()).thenReturn(1);
        when(userRepository.findById(1)).thenReturn(Optional.of(cardholder));
        when(cartRepository.findByUser_UserId(1)).thenReturn(Optional.of(cart));
        when(productRepository.findById(10)).thenReturn(Optional.of(redeemableProduct));
        when(cartItemRepository.findByCart_CartIdAndProduct_ProdId(100, 10))
                .thenReturn(Optional.empty());

        // ceiling is 500 * 2 = 1000; asking for 1200
        assertThatThrownBy(() -> cartService.addItem(CartItemRequest.builder()
                .prodId(10).quantity(2).redeemPoints(true).pointsUsed(1200).build()))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("exceeds the maximum");
    }

    @Test
    @DisplayName("points on a non-redeemable product are rejected")
    void nonRedeemableProductRejected() {
        ProductMaster plainProduct = ProductMaster.builder()
                .prodId(11).prodName("Atomic Habits")
                .mrpPrice(new BigDecimal("499.00"))
                .cardholderPrice(new BigDecimal("449.00"))
                .pointsToRedeem(0)
                .build();

        when(securityUtils.getCurrentUserId()).thenReturn(1);
        when(userRepository.findById(1)).thenReturn(Optional.of(cardholder));
        when(cartRepository.findByUser_UserId(1)).thenReturn(Optional.of(cart));
        when(productRepository.findById(11)).thenReturn(Optional.of(plainProduct));
        when(cartItemRepository.findByCart_CartIdAndProduct_ProdId(100, 11))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.addItem(CartItemRequest.builder()
                .prodId(11).quantity(1).redeemPoints(true).pointsUsed(50).build()))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("not eligible");
    }

    @Test
    @DisplayName("pointsUsed must be 0 when redeemPoints is false")
    void pointsWithoutRedeemFlagRejected() {
        when(securityUtils.getCurrentUserId()).thenReturn(1);
        when(userRepository.findById(1)).thenReturn(Optional.of(cardholder));
        when(cartRepository.findByUser_UserId(1)).thenReturn(Optional.of(cart));
        when(productRepository.findById(10)).thenReturn(Optional.of(redeemableProduct));
        when(cartItemRepository.findByCart_CartIdAndProduct_ProdId(100, 10))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.addItem(CartItemRequest.builder()
                .prodId(10).quantity(1).redeemPoints(false).pointsUsed(100).build()))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("must be 0");
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
        CartItem foreignItem = cartItem(55, redeemableProduct, 1, false, 0);
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
        CartItem foreignItem = cartItem(56, redeemableProduct, 1, false, 0);
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
        cart.getItems().add(cartItem(1, redeemableProduct, 2, false, 0));

        when(securityUtils.getCurrentUserId()).thenReturn(1);
        when(userRepository.findById(1)).thenReturn(Optional.of(cardholder));
        when(cartRepository.findByUser_UserId(1)).thenReturn(Optional.of(cart));
        when(cartRepository.findByUserIdWithItems(1)).thenReturn(Optional.of(cart));

        CartResponse response = cartService.clearCart();

        assertThat(cart.getItems()).isEmpty();
        assertThat(response.getTotalQuantity()).isZero();
        assertThat(response.getSubtotalPayable()).isEqualByComparingTo("0");
        verify(cartRepository).save(cart);
    }

    // ---------------------------------------------------------------

    private CartItem cartItem(Integer id, ProductMaster product, int qty,
                              boolean redeem, int points) {
        CartItem item = CartItem.builder()
                .cartItemId(id)
                .cart(cart)
                .product(product)
                .quantity(qty)
                .redeemPoints(redeem)
                .pointsUsed(points)
                .build();
        return item;
    }
}
