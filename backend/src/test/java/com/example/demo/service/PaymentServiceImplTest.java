package com.example.demo.service;

import com.example.demo.dto.request.PaymentVerifyRequest;
import com.example.demo.dto.response.PaymentResponse;
import com.example.demo.entity.*;
import com.example.demo.enums.*;
import com.example.demo.exception.BusinessRuleViolationException;
import com.example.demo.exception.UnauthorizedActionException;
import com.example.demo.mapper.PaymentMapper;
import com.example.demo.repository.EmartCardRepository;
import com.example.demo.repository.OrdersRepository;
import com.example.demo.repository.PaymentRepository;
import com.example.demo.security.SecurityUtils;
import com.example.demo.service.implementation.PaymentServiceImpl;
import com.example.demo.util.EmailUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PaymentServiceImplTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private OrdersRepository ordersRepository;
    @Mock private EmartCardRepository emartCardRepository;
    @Mock private EmailUtil emailUtil;
    @Mock private SecurityUtils securityUtils;
    @Mock private CardholderService cardholderService;

    private PaymentServiceImpl paymentService;

    private User cardholder;
    private User someoneElse;
    private Orders order;
    private EmartCard card;

    /** Mock gateway rule: a card ending in 0 is declined. */
    private static final String GOOD_CARD = "4242424242424242";
    private static final String BAD_CARD  = "4242424242424240";

    @BeforeEach
    void setUp() {
        paymentService = new PaymentServiceImpl(paymentRepository, ordersRepository,
                emartCardRepository, new PaymentMapper(), emailUtil, securityUtils,
                cardholderService);

        cardholder = User.builder().userId(1).firstName("Rishi").email("r@example.com")
                .membershipNo("EMART00001").isCardholder(true).isActive(true).build();
        someoneElse = User.builder().userId(2).firstName("Ananya").email("a@example.com")
                .membershipNo("EMART00002").isCardholder(false).isActive(true).build();

        Address addr = Address.builder().addressId(1).user(cardholder)
                .addressLine1("221B").city("Pune").state("MH").zipCode("411001")
                .country("India").addressType(AddressType.BOTH).isDefault(true).build();

        order = Orders.builder().orderId(5).orderNo("ORD-2026-000005").user(cardholder)
                .shippingAddress(addr).billingAddress(addr)
                .orderDate(java.time.LocalDateTime.now())
                .subtotalAmount(new BigDecimal("29999.00"))
                .totalAmount(new BigDecimal("31498.95"))
                .pointsRedeemed(200).pointsEarned(3149)
                .orderStatus(OrderStatus.PLACED).paymentStatus(PaymentStatus.PENDING)
                .items(new ArrayList<>()).build();

        card = EmartCard.builder().cardId(1).user(cardholder).cardNumber("EMCARD-1")
                .status(CardStatus.APPROVED).pointsBalance(1000).build();

        when(securityUtils.getCurrentUserId()).thenReturn(1);
        when(ordersRepository.findById(5)).thenReturn(Optional.of(order));
        when(ordersRepository.save(any(Orders.class))).thenAnswer(i -> i.getArgument(0));
        when(emartCardRepository.save(any(EmartCard.class))).thenAnswer(i -> i.getArgument(0));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> {
            Payment p = i.getArgument(0);
            p.setPaymentId(77);
            p.setTransactionDate(java.time.LocalDateTime.now());
            return p;
        });
    }

    private PaymentVerifyRequest req(String cardNumber, String amount) {
        return PaymentVerifyRequest.builder()
                .cardNumber(cardNumber).cardHolderName("RISHI C")
                .expiry("12/29").cvv("123").amount(new BigDecimal(amount)).build();
    }

    // ---------------- success ----------------

    @Test
    @DisplayName("successful payment marks the order PAID")
    void successMarksOrderPaid() {
        when(emartCardRepository.findByUser_UserId(1)).thenReturn(Optional.of(card));

        PaymentResponse r = paymentService.verifyPayment(5, req(GOOD_CARD, "31498.95"));

        assertThat(r.getStatus()).isEqualTo("SUCCESS");
        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.PAID);
    }

    @Test
    @DisplayName("points are debited and credited atomically: 1000 - 200 + 3149 = 3949")
    void pointsSettledCorrectly() {
        when(emartCardRepository.findByUser_UserId(1)).thenReturn(Optional.of(card));

        PaymentResponse r = paymentService.verifyPayment(5, req(GOOD_CARD, "31498.95"));

        assertThat(card.getPointsBalance()).isEqualTo(3949);
        assertThat(r.getPointsBalanceAfter()).isEqualTo(3949);
        assertThat(r.getPointsRedeemed()).isEqualTo(200);
        assertThat(r.getPointsEarned()).isEqualTo(3149);
    }

    @Test
    @DisplayName("only the last 4 card digits are ever stored")
    void onlyLast4Stored() {
        when(emartCardRepository.findByUser_UserId(1)).thenReturn(Optional.of(card));

        PaymentResponse r = paymentService.verifyPayment(5, req(GOOD_CARD, "31498.95"));

        assertThat(r.getCardLast4()).isEqualTo("4242");
        assertThat(r.getCardLast4()).hasSize(4);
    }

    @Test
    @DisplayName("a non-cardholder settles no points")
    void nonCardholderNoPoints() {
        order.setUser(someoneElse);
        when(securityUtils.getCurrentUserId()).thenReturn(2);
        when(emartCardRepository.findByUser_UserId(2)).thenReturn(Optional.empty());

        PaymentResponse r = paymentService.verifyPayment(5, req(GOOD_CARD, "31498.95"));

        assertThat(r.getStatus()).isEqualTo("SUCCESS");
        assertThat(r.getPointsBalanceAfter()).isNull();
        verify(emartCardRepository, never()).save(any());
    }

    // ---------------- failure ----------------

    @Test
    @DisplayName("a declined card leaves the order PENDING so it can be retried")
    void declineLeavesOrderRetryable() {
        PaymentResponse r = paymentService.verifyPayment(5, req(BAD_CARD, "31498.95"));

        assertThat(r.getStatus()).isEqualTo("FAILED");
        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.PLACED);
        verify(emartCardRepository, never()).save(any());
    }

    @Test
    @DisplayName("a failed attempt is still recorded for audit")
    void failedAttemptIsRecorded() {
        paymentService.verifyPayment(5, req(BAD_CARD, "31498.95"));
        verify(paymentRepository).save(any(Payment.class));
    }

    // ---------------- guards ----------------

    @Test
    @DisplayName("an amount that disagrees with the order total is refused")
    void amountMismatchRefused() {
        assertThatThrownBy(() -> paymentService.verifyPayment(5, req(GOOD_CARD, "1.00")))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("does not match");

        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("paying twice is blocked")
    void doublePaymentBlocked() {
        order.setPaymentStatus(PaymentStatus.PAID);

        assertThatThrownBy(() -> paymentService.verifyPayment(5, req(GOOD_CARD, "31498.95")))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("already been paid");
    }

    @Test
    @DisplayName("a cancelled order cannot be paid")
    void cancelledOrderCannotBePaid() {
        order.setOrderStatus(OrderStatus.CANCELLED);

        assertThatThrownBy(() -> paymentService.verifyPayment(5, req(GOOD_CARD, "31498.95")))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("cancelled");
    }

    @Test
    @DisplayName("you cannot pay for someone else's order")
    void foreignOrderBlocked() {
        order.setUser(someoneElse);   // caller is still userId 1

        assertThatThrownBy(() -> paymentService.verifyPayment(5, req(GOOD_CARD, "31498.95")))
                .isInstanceOf(UnauthorizedActionException.class);
    }

    @Test
    @DisplayName("points spent since checkout are re-checked at payment time")
    void balanceRecheckedAtPayment() {
        card.setPointsBalance(50);   // was 1000 at checkout, order wants 200
        when(emartCardRepository.findByUser_UserId(1)).thenReturn(Optional.of(card));

        assertThatThrownBy(() -> paymentService.verifyPayment(5, req(GOOD_CARD, "31498.95")))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("no longer enough");
    }
}
