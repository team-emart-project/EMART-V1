package com.example.demo.service;

import com.example.demo.entity.EmartCard;
import com.example.demo.entity.User;
import com.example.demo.enums.CardStatus;
import com.example.demo.repository.EmartCardRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * THE single source of truth for "does this user hold an active e-MART card?"
 *
 * WHY THIS CLASS EXISTS
 * ---------------------
 * The schema stores that fact in TWO places:
 *
 *     users.is_cardholder     (drives which price the cart charges)
 *     emart_card.status       (drives whether e-Points are settled)
 *
 * Nothing kept them in sync, and nothing ever SET either of them, which is the
 * root cause of the "e-Points never update" bug:
 *
 *   - apply() wrote status = PENDING and left is_cardholder = false
 *   - PaymentServiceImpl.settlePoints() bailed out unless status == APPROVED
 *   - OrderServiceImpl reported a redeemable balance of 0 for the same reason
 *
 * So for every user who applied through the app, points were silently never
 * credited and redemption always failed. Only the hand-seeded APPROVED row
 * worked. Routing every read AND write through this class means the two columns
 * can no longer disagree.
 */
@Service
public class CardholderService {

    private static final Logger log = LoggerFactory.getLogger(CardholderService.class);

    private final EmartCardRepository emartCardRepository;
    private final UserRepository userRepository;
    private final SecurityUtils securityUtils;

    public CardholderService(EmartCardRepository emartCardRepository,
                             UserRepository userRepository,
                             SecurityUtils securityUtils) {
        this.emartCardRepository = emartCardRepository;
        this.userRepository = userRepository;
        this.securityUtils = securityUtils;
    }

    /** An "active" card is an APPROVED one. PENDING and REJECTED do not count. */
    @Transactional(readOnly = true)
    public boolean isActiveCardholder(Integer userId) {
        if (userId == null) return false;
        return emartCardRepository.findByUser_UserId(userId)
                .map(c -> c.getStatus() == CardStatus.APPROVED)
                .orElse(false);
    }

    /** For PUBLIC endpoints: false for a signed-out visitor, no exception. */
    @Transactional(readOnly = true)
    public boolean isCurrentUserCardholder() {
        return isActiveCardholder(securityUtils.getCurrentUserIdOrNull());
    }

    /** Redeemable e-Points. 0 for non-cardholders — never null, never throws. */
    @Transactional(readOnly = true)
    public int getPointsBalance(Integer userId) {
        if (userId == null) return 0;
        return emartCardRepository.findByUser_UserId(userId)
                .filter(c -> c.getStatus() == CardStatus.APPROVED)
                .map(EmartCard::getPointsBalance)
                .orElse(0);
    }

    @Transactional(readOnly = true)
    public int getCurrentUserPointsBalance() {
        return getPointsBalance(securityUtils.getCurrentUserIdOrNull());
    }

    @Transactional(readOnly = true)
    public Optional<EmartCard> findCard(Integer userId) {
        return emartCardRepository.findByUser_UserId(userId);
    }

    /**
     * Approves a card and flips users.is_cardholder in the SAME transaction.
     *
     * This is the only method allowed to set either value, which is what stops
     * the two columns drifting apart again.
     */
    @Transactional
    public EmartCard approve(EmartCard card, java.time.LocalDate approvalDate) {
        card.setStatus(CardStatus.APPROVED);
        card.setApprovalDate(approvalDate);
        emartCardRepository.save(card);

        User user = card.getUser();
        user.setIsCardholder(true);
        userRepository.save(user);

        log.info("Card {} APPROVED for userId={}; users.is_cardholder set to true",
                card.getCardNumber(), user.getUserId());
        return card;
    }

    /**
     * Applies a points delta and persists it.
     *
     * Callers used to mutate the entity and rely on the transaction flushing it,
     * which works — but doing the save here keeps the "who is allowed to change
     * a balance" answer in one file.
     */
    @Transactional
    public int adjustPoints(EmartCard card, int redeemed, int earned) {
        int newBalance = card.getPointsBalance() - redeemed + earned;
        card.setPointsBalance(newBalance);
        emartCardRepository.save(card);
        log.info("Points settled for cardId={}: -{} +{} => {}",
                card.getCardId(), redeemed, earned, newBalance);
        return newBalance;
    }
}
