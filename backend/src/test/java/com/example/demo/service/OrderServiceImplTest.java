package com.example.demo.service;

import com.example.demo.dto.request.CheckoutRequest;
import com.example.demo.dto.response.OrderResponse;
import com.example.demo.entity.*;
import com.example.demo.enums.*;
import com.example.demo.exception.BusinessRuleViolationException;
import com.example.demo.exception.UnauthorizedActionException;
import com.example.demo.mapper.AddressMapper;
import com.example.demo.mapper.OrderMapper;
import com.example.demo.repository.*;
import com.example.demo.security.SecurityUtils;
import com.example.demo.service.CardholderService;
import com.example.demo.service.implementation.OrderServiceImpl;
import com.example.demo.util.InvoicePdfGenerator;
import com.example.demo.util.OrderNumberGeneratorUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrderServiceImplTest {

    @Mock private OrdersRepository ordersRepository;
    @Mock private CartRepository cartRepository;
    @Mock private CartItemRepository cartItemRepository;
    @Mock private AddressRepository addressRepository;
    @Mock private UserRepository userRepository;
    @Mock private EmartCardRepository emartCardRepository;
    @Mock private OrderNumberGeneratorUtil orderNumberGenerator;
    @Mock private InvoicePdfGenerator invoicePdfGenerator;
    @Mock private SecurityUtils securityUtils;
    @Mock private CardholderService cardholderService;

    private final OrderMapper orderMapper = new OrderMapper(new AddressMapper());
    private final PricingService pricingService = new PricingService();

    /**
     * Built by hand rather than with @InjectMocks: OrderMapper is a REAL object
     * (its arithmetic is part of what we are testing), and @InjectMocks would
     * pass null for any constructor argument that is not a @Mock.
     */
    private OrderServiceImpl orderService;

    private User cardholder;
    private User normalUser;
    private Address address;
    private ProductMaster product;
    private Cart cart;

    @BeforeEach
    void setUp() {
        orderService = new OrderServiceImpl(ordersRepository, cartRepository, cartItemRepository,
                addressRepository, userRepository, emartCardRepository, orderMapper,
                orderNumberGenerator, invoicePdfGenerator, securityUtils, cardholderService,
                pricingService);

        // @Value fields are populated by Spring at runtime; set them manually here.
        ReflectionTestUtils.setField(pricingService, "requireCardholder", true);
        ReflectionTestUtils.setField(orderService, "tier1Limit", new BigDecimal("5000"));
        ReflectionTestUtils.setField(orderService, "tier2Limit", new BigDecimal("25000"));
        ReflectionTestUtils.setField(orderService, "tier3Limit", new BigDecimal("75000"));
        ReflectionTestUtils.setField(orderService, "tier1Pct", new BigDecimal("2"));
        ReflectionTestUtils.setField(orderService, "tier2Pct", new BigDecimal("3"));
        ReflectionTestUtils.setField(orderService, "tier3Pct", new BigDecimal("4"));
        ReflectionTestUtils.setField(orderService, "tier4Pct", new BigDecimal("5"));

        cardholder = User.builder().userId(1).firstName("Rishi").lastName("C")
                .membershipNo("EMART00001").isCardholder(true).isActive(true).build();
        normalUser = User.builder().userId(2).firstName("Ananya").lastName("S")
                .membershipNo("EMART00002").isCardholder(false).isActive(true).build();

        address = Address.builder().addressId(1).user(cardholder)
                .addressLine1("221B Baker Street").city("Pune").state("Maharashtra")
                .zipCode("411001").country("India").addressType(AddressType.BOTH)
                .isDefault(true).build();

        product = ProductMaster.builder().prodId(10).prodName("Canon EOS 1500D")
                .mrpPrice(new BigDecimal("33000.00"))
                .cardholderPrice(new BigDecimal("30000.00"))
                .hybridCashPrice(new BigDecimal("20000.00"))
                .hybridPoints(500)
                .build();

        cart = Cart.builder().cartId(100).user(cardholder)
                .status(CartStatus.ACTIVE).items(new ArrayList<>()).build();

        when(securityUtils.getCurrentUserId()).thenReturn(1);
        when(userRepository.findById(1)).thenReturn(Optional.of(cardholder));
        when(cardholderService.isActiveCardholder(1)).thenReturn(true);
        when(cardholderService.isActiveCardholder(2)).thenReturn(false);
        when(cardholderService.getPointsBalance(1)).thenReturn(1000);
        when(addressRepository.findById(1)).thenReturn(Optional.of(address));
        when(cartRepository.findByUserIdWithItems(1)).thenReturn(Optional.of(cart));
        when(ordersRepository.save(any(Orders.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderNumberGenerator.generate()).thenReturn("ORD-2026-000042");
    }

    private CartItem line(int qty, PriceOption option) {
        return CartItem.builder().cartItemId(1).cart(cart).product(product)
                .quantity(qty).priceOption(option).pointsUsed(0).build();
    }

    // ---------------- maths ----------------

    @Test
    @DisplayName("MEMBER option charges the member price, adds NO tax, earns tier-3 (4%)")
    void cardholderMaths() {
        cart.getItems().add(line(2, PriceOption.MEMBER));

        OrderResponse r = orderService.checkoutPreview(
                CheckoutRequest.builder().shippingAddressId(1).build());

        // 30000 * 2 = 60000
        assertThat(r.getSubtotalAmount()).isEqualByComparingTo("60000.00");
        // 33000 * 2 = 66000 -> savings 6000
        assertThat(r.getSubtotalMrp()).isEqualByComparingTo("66000.00");
        assertThat(r.getTotalSavings()).isEqualByComparingTo("6000.00");
        // NO TAX: the total is exactly the subtotal
        assertThat(r.getTotalAmount()).isEqualByComparingTo("60000.00");
        // 60,000 is under 75,000 -> tier 3 = 4%, rounded down
        assertThat(r.getPointsEarned()).isEqualTo(2400);
        assertThat(r.getPreview()).isTrue();
    }

    @Test
    @DisplayName("a non-cardholder pays MRP and earns no points")
    void normalUserMaths() {
        Cart plainCart = Cart.builder().cartId(200).user(normalUser)
                .status(CartStatus.ACTIVE).items(new ArrayList<>()).build();
        plainCart.getItems().add(CartItem.builder().cartItemId(2).cart(plainCart)
                .product(product).quantity(1).priceOption(PriceOption.REGULAR).pointsUsed(0).build());

        Address addr2 = Address.builder().addressId(2).user(normalUser)
                .addressLine1("45 Lake View").city("Mumbai").state("MH")
                .zipCode("400001").country("India").addressType(AddressType.BOTH)
                .isDefault(true).build();

        when(securityUtils.getCurrentUserId()).thenReturn(2);
        when(userRepository.findById(2)).thenReturn(Optional.of(normalUser));
        when(cartRepository.findByUserIdWithItems(2)).thenReturn(Optional.of(plainCart));
        when(addressRepository.findById(2)).thenReturn(Optional.of(addr2));

        OrderResponse r = orderService.checkoutPreview(
                CheckoutRequest.builder().shippingAddressId(2).build());

        assertThat(r.getSubtotalAmount()).isEqualByComparingTo("33000.00");
        assertThat(r.getTotalSavings()).isEqualByComparingTo("0.00");
        assertThat(r.getPointsEarned()).isZero();
    }

    @Test
    @DisplayName("HYBRID charges only the cash half; points are not a discount")
    void hybridChargesCashHalfOnly() {
        cart.getItems().add(line(1, PriceOption.HYBRID));
        when(cardholderService.getPointsBalance(1)).thenReturn(1000);

        OrderResponse r = orderService.checkoutPreview(
                CheckoutRequest.builder().shippingAddressId(1).build());

        // The points are part of the PRICE, not subtracted afterwards: the
        // shopper pays 20000 cash and 500 points, and the total is 20000 flat.
        assertThat(r.getPointsRedeemed()).isEqualTo(500);
        assertThat(r.getSubtotalAmount()).isEqualByComparingTo("20000.00");
        assertThat(r.getTotalAmount()).isEqualByComparingTo("20000.00");
    }

    @Test
    @DisplayName("spending more points than the balance is blocked")
    void overRedemptionBlocked() {
        cart.getItems().add(line(1, PriceOption.HYBRID));
        when(cardholderService.getPointsBalance(1)).thenReturn(100);

        assertThatThrownBy(() -> orderService.checkoutPreview(
                CheckoutRequest.builder().shippingAddressId(1).build()))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("100");
    }

    // ---------------- tiered e-Points earning (2% - 5%) ----------------

    @Test
    @DisplayName("earn tiers: 2% under 5k, 3% under 25k, 4% under 75k, 5% at/above 75k")
    void earnTiersAcrossBoundaries() {
        // just below / just above each boundary, plus the rounding-down rule
        assertThat(orderService.calculatePointsEarned(new BigDecimal("1000.00"))).isEqualTo(20);    // 2%
        assertThat(orderService.calculatePointsEarned(new BigDecimal("4999.99"))).isEqualTo(99);    // 2%
        assertThat(orderService.calculatePointsEarned(new BigDecimal("5000.00"))).isEqualTo(150);   // 3%
        assertThat(orderService.calculatePointsEarned(new BigDecimal("24999.99"))).isEqualTo(749);  // 3%
        assertThat(orderService.calculatePointsEarned(new BigDecimal("25000.00"))).isEqualTo(1000); // 4%
        assertThat(orderService.calculatePointsEarned(new BigDecimal("74999.99"))).isEqualTo(2999); // 4%
        assertThat(orderService.calculatePointsEarned(new BigDecimal("75000.00"))).isEqualTo(3750); // 5%
        assertThat(orderService.calculatePointsEarned(new BigDecimal("200000.00"))).isEqualTo(10000);
    }

    @Test
    @DisplayName("earned points are always rounded DOWN, never up")
    void earnRoundsDown() {
        // 1234.56 * 2% = 24.6912 -> 24
        assertThat(orderService.calculatePointsEarned(new BigDecimal("1234.56"))).isEqualTo(24);
    }

    @Test
    @DisplayName("a zero or negative total earns nothing")
    void earnOnZeroTotal() {
        assertThat(orderService.calculatePointsEarned(BigDecimal.ZERO)).isZero();
        assertThat(orderService.calculatePointsEarned(null)).isZero();
    }

    @Test
    @DisplayName("a NON-cardholder earns no points regardless of order size")
    void nonCardholderEarnsNothingEvenOnBigOrder() {
        Cart plainCart = Cart.builder().cartId(200).user(normalUser)
                .status(CartStatus.ACTIVE).items(new ArrayList<>()).build();
        plainCart.getItems().add(CartItem.builder().cartItemId(9).cart(plainCart)
                .product(product).quantity(5).priceOption(PriceOption.REGULAR).pointsUsed(0).build());

        Address addr2 = Address.builder().addressId(2).user(normalUser)
                .addressLine1("45 Lake View").city("Mumbai").state("MH")
                .zipCode("400001").country("India").addressType(AddressType.BOTH)
                .isDefault(true).build();

        when(securityUtils.getCurrentUserId()).thenReturn(2);
        when(userRepository.findById(2)).thenReturn(Optional.of(normalUser));
        when(cartRepository.findByUserIdWithItems(2)).thenReturn(Optional.of(plainCart));
        when(addressRepository.findById(2)).thenReturn(Optional.of(addr2));

        OrderResponse r = orderService.checkoutPreview(
                CheckoutRequest.builder().shippingAddressId(2).build());

        assertThat(r.getTotalAmount()).isGreaterThan(new BigDecimal("75000"));
        assertThat(r.getPointsEarned()).isZero();
    }

    // ---------------- placing ----------------

    @Test
    @DisplayName("an empty cart cannot be checked out")
    void emptyCartBlocked() {
        assertThatThrownBy(() -> orderService.placeOrder(
                CheckoutRequest.builder().shippingAddressId(1).build()))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("cart is empty");
    }

    @Test
    @DisplayName("placing an order snapshots the product name and empties the cart")
    void placeOrderSnapshotsAndClearsCart() {
        cart.getItems().add(line(2, PriceOption.REGULAR));

        OrderResponse r = orderService.placeOrder(
                CheckoutRequest.builder().shippingAddressId(1).build());

        ArgumentCaptor<Orders> captor = ArgumentCaptor.forClass(Orders.class);
        verify(ordersRepository).save(captor.capture());
        Orders saved = captor.getValue();

        assertThat(saved.getItems()).hasSize(1);
        assertThat(saved.getItems().get(0).getProdNameSnapshot()).isEqualTo("Canon EOS 1500D");
        assertThat(saved.getOrderStatus()).isEqualTo(OrderStatus.PLACED);
        assertThat(saved.getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(r.getOrderNo()).isEqualTo("ORD-2026-000042");

        // cart row is REUSED (user_id is UNIQUE), just emptied
        assertThat(cart.getItems()).isEmpty();
        assertThat(cart.getStatus()).isEqualTo(CartStatus.ACTIVE);
    }

    @Test
    @DisplayName("checking out with someone else's address is blocked")
    void foreignAddressBlocked() {
        cart.getItems().add(line(1, PriceOption.REGULAR));
        Address foreign = Address.builder().addressId(9).user(normalUser)
                .addressLine1("x").city("c").state("s").zipCode("z").country("India")
                .addressType(AddressType.BOTH).isDefault(false).build();
        when(addressRepository.findById(9)).thenReturn(Optional.of(foreign));

        assertThatThrownBy(() -> orderService.placeOrder(
                CheckoutRequest.builder().shippingAddressId(9).build()))
                .isInstanceOf(UnauthorizedActionException.class);
    }

    @Test
    @DisplayName("billing address defaults to the shipping address")
    void billingDefaultsToShipping() {
        cart.getItems().add(line(1, PriceOption.REGULAR));

        orderService.placeOrder(CheckoutRequest.builder().shippingAddressId(1).build());

        ArgumentCaptor<Orders> captor = ArgumentCaptor.forClass(Orders.class);
        verify(ordersRepository).save(captor.capture());
        assertThat(captor.getValue().getBillingAddress().getAddressId()).isEqualTo(1);
    }

    // ---------------- cancel ----------------

    @Test
    @DisplayName("a PLACED + PENDING order can be cancelled")
    void cancelAllowed() {
        Orders order = order(OrderStatus.PLACED, PaymentStatus.PENDING);
        when(ordersRepository.findByIdWithItems(5)).thenReturn(Optional.of(order));

        OrderResponse r = orderService.cancelOrder(5);

        assertThat(r.getOrderStatus()).isEqualTo("CANCELLED");
    }

    @Test
    @DisplayName("a PAID order cannot be cancelled")
    void cancelPaidBlocked() {
        Orders order = order(OrderStatus.PAID, PaymentStatus.PAID);
        when(ordersRepository.findByIdWithItems(6)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelOrder(6))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("refund");
    }

    @Test
    @DisplayName("cancelling twice is blocked")
    void doubleCancelBlocked() {
        Orders order = order(OrderStatus.CANCELLED, PaymentStatus.PENDING);
        when(ordersRepository.findByIdWithItems(7)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelOrder(7))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("already cancelled");
    }

    // ---------------- invoice ----------------

    @Test
    @DisplayName("the invoice is refused until the order is paid")
    void invoiceBlockedBeforePayment() {
        Orders order = order(OrderStatus.PLACED, PaymentStatus.PENDING);
        when(ordersRepository.findByIdWithItems(8)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.generateInvoicePdf(8))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("once the order has been paid");

        verify(invoicePdfGenerator, never()).generate(any());
    }

    @Test
    @DisplayName("the invoice is produced once the order is paid")
    void invoiceAllowedAfterPayment() {
        Orders order = order(OrderStatus.PAID, PaymentStatus.PAID);
        when(ordersRepository.findByIdWithItems(9)).thenReturn(Optional.of(order));
        when(invoicePdfGenerator.generate(order)).thenReturn(new byte[]{1, 2, 3});

        assertThat(orderService.generateInvoicePdf(9)).hasSize(3);
    }

    @Test
    @DisplayName("you cannot read someone else's order")
    void foreignOrderBlocked() {
        Orders foreign = Orders.builder().orderId(11).orderNo("ORD-X").user(normalUser)
                .shippingAddress(address).billingAddress(address)
                .subtotalAmount(BigDecimal.TEN)
                .totalAmount(BigDecimal.TEN).pointsRedeemed(0).pointsEarned(0)
                .orderStatus(OrderStatus.PLACED).paymentStatus(PaymentStatus.PENDING)
                .items(new ArrayList<>()).build();
        when(ordersRepository.findByIdWithItems(11)).thenReturn(Optional.of(foreign));

        assertThatThrownBy(() -> orderService.getOrder(11))
                .isInstanceOf(UnauthorizedActionException.class);
    }

    private Orders order(OrderStatus os, PaymentStatus ps) {
        Orders o = Orders.builder()
                .orderId(5).orderNo("ORD-2026-000005").user(cardholder)
                .shippingAddress(address).billingAddress(address)
                .orderDate(java.time.LocalDateTime.now())
                .subtotalAmount(new BigDecimal("30000.00"))
                .totalAmount(new BigDecimal("30000.00"))
                .pointsRedeemed(0).pointsEarned(1200)
                .orderStatus(os).paymentStatus(ps)
                .items(new ArrayList<>()).build();
        o.addItem(OrderDetail.builder().orderDtlId(1).product(product)
                .prodNameSnapshot("Canon EOS 1500D").quantity(1)
                .mrpPrice(new BigDecimal("33000.00"))
                .cardholderPrice(new BigDecimal("30000.00"))
                .priceOption(PriceOption.MEMBER)
                .priceCharged(new BigDecimal("30000.00"))
                .pointsRedeemed(0).build());
        return o;
    }
}
