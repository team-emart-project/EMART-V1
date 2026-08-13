package com.example.demo.service;

import com.example.demo.entity.EmartCard;
import com.example.demo.entity.User;
import com.example.demo.enums.CardStatus;
import com.example.demo.repository.EmartCardRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Guards the fix for the "e-Points never update" bug.
 *
 * The original defect: a card stayed PENDING forever and users.is_cardholder
 * was never set, so every points check silently no-opped.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CardholderServiceTest {

    @Mock private EmartCardRepository emartCardRepository;
    @Mock private UserRepository userRepository;
    @Mock private SecurityUtils securityUtils;

    @InjectMocks private CardholderService cardholderService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder().userId(1).email("a@b.com").isCardholder(false).isActive(true).build();
        when(emartCardRepository.save(any(EmartCard.class))).thenAnswer(i -> i.getArgument(0));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
    }

    private EmartCard card(CardStatus status, int points) {
        return EmartCard.builder().cardId(1).user(user).cardNumber("EMCARD-1")
                .status(status).pointsBalance(points).applicationDate(LocalDate.now()).build();
    }

    @Test
    @DisplayName("only an APPROVED card counts as an active cardholder")
    void onlyApprovedCounts() {
        when(emartCardRepository.findByUser_UserId(1)).thenReturn(Optional.of(card(CardStatus.APPROVED, 500)));
        assertThat(cardholderService.isActiveCardholder(1)).isTrue();

        when(emartCardRepository.findByUser_UserId(1)).thenReturn(Optional.of(card(CardStatus.PENDING, 500)));
        assertThat(cardholderService.isActiveCardholder(1)).isFalse();

        when(emartCardRepository.findByUser_UserId(1)).thenReturn(Optional.of(card(CardStatus.REJECTED, 500)));
        assertThat(cardholderService.isActiveCardholder(1)).isFalse();
    }

    @Test
    @DisplayName("no card at all is not an error - just not a cardholder")
    void noCardIsNotAnError() {
        when(emartCardRepository.findByUser_UserId(1)).thenReturn(Optional.empty());
        assertThat(cardholderService.isActiveCardholder(1)).isFalse();
        assertThat(cardholderService.getPointsBalance(1)).isZero();
    }

    @Test
    @DisplayName("a signed-out visitor (null userId) is never a cardholder")
    void nullUserIsSafe() {
        assertThat(cardholderService.isActiveCardholder(null)).isFalse();
        assertThat(cardholderService.getPointsBalance(null)).isZero();
    }

    @Test
    @DisplayName("a PENDING card reports a ZERO redeemable balance even if points exist")
    void pendingCardHasNoRedeemableBalance() {
        when(emartCardRepository.findByUser_UserId(1)).thenReturn(Optional.of(card(CardStatus.PENDING, 900)));
        assertThat(cardholderService.getPointsBalance(1)).isZero();
    }

    // ---------------- the actual bug fix ----------------

    @Test
    @DisplayName("approve() sets status APPROVED *and* users.is_cardholder together")
    void approveSyncsBothSourcesOfTruth() {
        EmartCard pending = card(CardStatus.PENDING, 0);

        cardholderService.approve(pending, LocalDate.of(2026, 7, 28));

        assertThat(pending.getStatus()).isEqualTo(CardStatus.APPROVED);
        assertThat(pending.getApprovalDate()).isEqualTo(LocalDate.of(2026, 7, 28));
        // THE bug: this flag was never set, so cardholder pricing never applied
        assertThat(user.getIsCardholder()).isTrue();

        verify(emartCardRepository).save(pending);
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("adjustPoints debits redeemed and credits earned in one step")
    void adjustPointsMovesTheBalance() {
        EmartCard approved = card(CardStatus.APPROVED, 1000);

        int after = cardholderService.adjustPoints(approved, 200, 350);

        assertThat(after).isEqualTo(1150);              // 1000 - 200 + 350
        assertThat(approved.getPointsBalance()).isEqualTo(1150);
        verify(emartCardRepository).save(approved);     // the missing persist
    }

    @Test
    @DisplayName("earning with nothing redeemed still increases the balance")
    void earnOnlyIncreasesBalance() {
        EmartCard approved = card(CardStatus.APPROVED, 40);
        assertThat(cardholderService.adjustPoints(approved, 0, 60)).isEqualTo(100);
    }
}
