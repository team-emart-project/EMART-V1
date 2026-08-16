package com.example.demo.service.implementation;

import com.example.demo.dto.request.PaymentVerifyRequest;
import com.example.demo.dto.response.PaymentResponse;
import com.example.demo.entity.EmartCard;
import com.example.demo.entity.Orders;
import com.example.demo.entity.Payment;
import com.example.demo.enums.CardStatus;
import com.example.demo.enums.OrderStatus;
import com.example.demo.enums.PaymentStatus;
import com.example.demo.exception.BusinessRuleViolationException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.exception.UnauthorizedActionException;
import com.example.demo.mapper.PaymentMapper;
import com.example.demo.repository.EmartCardRepository;
import com.example.demo.repository.OrdersRepository;
import com.example.demo.repository.PaymentRepository;
import com.example.demo.security.SecurityUtils;
import com.example.demo.service.CardholderService;
import com.example.demo.service.interfaces.PaymentService;
import com.example.demo.util.EmailUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Mock payment gateway.
 *
 * A real integration would delegate to Stripe/Razorpay and verify a webhook
 * signature. Everything AROUND the gateway call — ownership, amount matching,
 * state transitions, points settlement — is real and is what matters here.
 */
@Service
@Transactional(readOnly = true)
public class PaymentServiceImpl implements PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentServiceImpl.class);

    /** Mock rule: a card number ending in 0 is declined, so failure is testable. */
    private static final String DECLINE_SUFFIX = "0";

    private final PaymentRepository paymentRepository;
    private final OrdersRepository ordersRepository;
    private final EmartCardRepository emartCardRepository;
    private final PaymentMapper paymentMapper;
    private final EmailUtil emailUtil;
    private final SecurityUtils securityUtils;
    private final CardholderService cardholderService;

    public PaymentServiceImpl(PaymentRepository paymentRepository,
                              OrdersRepository ordersRepository,
                              EmartCardRepository emartCardRepository,
                              PaymentMapper paymentMapper,
                              EmailUtil emailUtil,
                              SecurityUtils securityUtils,
                              CardholderService cardholderService) {
        this.paymentRepository = paymentRepository;
        this.ordersRepository = ordersRepository;
        this.emartCardRepository = emartCardRepository;
        this.paymentMapper = paymentMapper;
        this.emailUtil = emailUtil;
        this.securityUtils = securityUtils;
        this.cardholderService = cardholderService;
    }

    @Override
    @Transactional
    public PaymentResponse verifyPayment(Integer orderId, PaymentVerifyRequest request) {

        Orders order = loadOwnedOrder(orderId);

        if (order.getOrderStatus() == OrderStatus.CANCELLED) {
            throw new BusinessRuleViolationException("This order has been cancelled");
        }
        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            throw new BusinessRuleViolationException("This order has already been paid");
        }

        // The client sends the amount it thinks it owes. If that disagrees with
        // the server's figure, something was tampered with — refuse.
        if (request.getAmount().compareTo(order.getTotalAmount()) != 0) {
            throw new BusinessRuleViolationException(
                    "Amount %s does not match the order total %s"
                            .formatted(request.getAmount(), order.getTotalAmount()));
        }

        // An order fully covered by e-Points has nothing to charge, so no card is
        // required and none is asked for. This check lives here rather than as a
        // @NotBlank on the DTO because whether a card is needed depends on the
        // ORDER, which a per-field annotation cannot see.
        boolean nothingToPay = order.getTotalAmount().signum() == 0;

        String cardNumber = request.getCardNumber() == null ? "" : request.getCardNumber().trim();
        String last4 = null;
        boolean approved = true;

        if (!nothingToPay) {
            if (cardNumber.isBlank()) {
                throw new BusinessRuleViolationException("Card details are required to pay this order");
            }
            if (request.getCardHolderName() == null || request.getCardHolderName().isBlank()) {
                throw new BusinessRuleViolationException("Card holder name is required");
            }
            last4 = cardNumber.substring(cardNumber.length() - 4);
            approved = !cardNumber.endsWith(DECLINE_SUFFIX);
        }

        Payment payment = Payment.builder()
                .order(order)
                .paymentMethod(nothingToPay ? "POINTS" : "CARD")
                // Only the last 4 digits are stored. Never the full number, never the CVV.
                .cardLast4(last4)
                .amount(order.getTotalAmount())
                .status(approved ? PaymentStatus.SUCCESS : PaymentStatus.FAILED)
                .transactionRef("TXN-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase())
                .build();

        Payment savedPayment = paymentRepository.save(payment);

        if (!approved) {
            // Leave the order PENDING so the customer can retry with another card.
            log.info("Payment DECLINED for orderNo={}", order.getOrderNo());
            return paymentMapper.toResponse(savedPayment);
        }

        // ---- success path: everything below is in ONE transaction ----
        order.setPaymentStatus(PaymentStatus.PAID);
        order.setOrderStatus(OrderStatus.PAID);
        ordersRepository.save(order);

        Integer balanceAfter = settlePoints(order);

        emailUtil.sendMembershipNumber(
                order.getUser().getEmail(),
                order.getUser().getFirstName(),
                order.getUser().getMembershipNo());

        log.info("Payment SUCCESS for orderNo={} amount={}",
                order.getOrderNo(), order.getTotalAmount());

        PaymentResponse response = paymentMapper.toResponse(savedPayment);
        response.setPointsEarned(order.getPointsEarned());
        response.setPointsRedeemed(order.getPointsRedeemed());
        response.setPointsBalanceAfter(balanceAfter);
        response.setOrderStatus(order.getOrderStatus().name());
        return response;
    }

    @Override
    public List<PaymentResponse> getPaymentsForOrder(Integer orderId) {
        loadOwnedOrder(orderId);   // ownership check before exposing anything
        return paymentRepository.findByOrder_OrderIdOrderByTransactionDateDesc(orderId).stream()
                .map(paymentMapper::toResponse)
                .toList();
    }

    // ------------------------------------------------------------------

    /**
     * Debits redeemed points and credits earned points, in the same transaction
     * as the order update — so the balance can never drift from the order.
     */
    private Integer settlePoints(Orders order) {

        Optional<EmartCard> maybeCard =
                cardholderService.findCard(order.getUser().getUserId());

        if (maybeCard.isEmpty() || maybeCard.get().getStatus() != CardStatus.APPROVED) {
            // Not an active cardholder: nothing earned, nothing to debit.
            //
            // This branch used to swallow EVERY user, because apply() left the
            // card at PENDING and nothing ever approved it. CardholderService
            // now approves on application, so this only skips genuine
            // non-cardholders.
            log.debug("userId={} has no APPROVED card - no points settled",
                    order.getUser().getUserId());
            return null;
        }

        EmartCard card = maybeCard.get();
        int redeemed = order.getPointsRedeemed() == null ? 0 : order.getPointsRedeemed();
        int earned = order.getPointsEarned() == null ? 0 : order.getPointsEarned();

        // Re-check at payment time: the balance may have changed since checkout.
        if (redeemed > card.getPointsBalance()) {
            throw new BusinessRuleViolationException(
                    "Your e-Points balance (%d) is no longer enough to redeem %d points"
                            .formatted(card.getPointsBalance(), redeemed));
        }

        // Debit + credit in one place, inside this transaction, so the balance
        // and the order can never disagree.
        return cardholderService.adjustPoints(card, redeemed, earned);
    }

    private Orders loadOwnedOrder(Integer orderId) {
        Orders order = ordersRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "orderId", orderId));

        if (!order.getUser().getUserId().equals(securityUtils.getCurrentUserId())) {
            throw new UnauthorizedActionException("That order does not belong to you");
        }
        return order;
    }
}
