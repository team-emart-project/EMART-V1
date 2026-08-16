package com.example.demo.service;

import com.example.demo.dto.request.EmartCardApplicationRequest;
import com.example.demo.dto.response.EmartCardResponse;
import com.example.demo.entity.EmartCard;
import com.example.demo.entity.User;
import com.example.demo.enums.CardStatus;
import com.example.demo.exception.DuplicateResourceException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.mapper.EmartCardMapper;
import com.example.demo.repository.EmartCardRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.SecurityUtils;
import com.example.demo.service.implementation.EmartCardServiceImpl;
import com.example.demo.util.CardNumberGeneratorUtil;
import com.example.demo.util.EmailUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EmartCardServiceImplTest {

    @Mock private EmartCardRepository emartCardRepository;
    @Mock private UserRepository userRepository;
    @Mock private CardNumberGeneratorUtil cardNumberGenerator;
    @Mock private EmailUtil emailUtil;
    @Mock private SecurityUtils securityUtils;
    @Spy  private EmartCardMapper emartCardMapper = new EmartCardMapper();

    @InjectMocks private EmartCardServiceImpl emartCardService;

    private User me;

    @BeforeEach
    void setUp() {
        me = User.builder().userId(1).firstName("Rishi").email("rishi@example.com")
                .isCardholder(false).isActive(true).build();

        when(securityUtils.getCurrentUserId()).thenReturn(1);
        when(userRepository.findById(1)).thenReturn(Optional.of(me));
        when(cardNumberGenerator.generate()).thenReturn("EMCARD-1234567");
        when(emartCardRepository.save(any(EmartCard.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("a new application is saved as PENDING with a zero points balance")
    void applicationStartsPending() {
        when(emartCardRepository.existsByUser_UserId(1)).thenReturn(false);

        EmartCardResponse response = emartCardService.apply(request());

        assertThat(response.getStatus()).isEqualTo("PENDING");
        assertThat(response.getPointsBalance()).isZero();
        assertThat(response.getApprovalDate()).isNull();
    }

    @Test
    @DisplayName("applying does NOT make the user a cardholder - approval does")
    void applyingDoesNotGrantCardholderPricing() {
        when(emartCardRepository.existsByUser_UserId(1)).thenReturn(false);

        emartCardService.apply(request());

        // critical: cart pricing keys off this flag
        assertThat(me.getIsCardholder()).isFalse();
    }

    @Test
    @DisplayName("a second application is rejected")
    void duplicateApplicationRejected() {
        when(emartCardRepository.existsByUser_UserId(1)).thenReturn(true);

        assertThatThrownBy(() -> emartCardService.apply(request()))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already applied");

        verify(emartCardRepository, never()).save(any());
    }

    @Test
    @DisplayName("PAN is stored uppercased")
    void panIsUppercased() {
        when(emartCardRepository.existsByUser_UserId(1)).thenReturn(false);

        emartCardService.apply(EmartCardApplicationRequest.builder()
                .employmentDetails("Engineer").bankAccountNo("123456789012")
                .panNumber("abcde1234f").build());

        ArgumentCaptor<EmartCard> captor = ArgumentCaptor.forClass(EmartCard.class);
        verify(emartCardRepository).save(captor.capture());
        assertThat(captor.getValue().getPanNumber()).isEqualTo("ABCDE1234F");
    }

    @Test
    @DisplayName("the response masks the bank account and never returns the PAN")
    void sensitiveFieldsAreProtected() {
        when(emartCardRepository.existsByUser_UserId(1)).thenReturn(false);

        EmartCardResponse response = emartCardService.apply(request());

        assertThat(response.getBankAccountMasked()).isEqualTo("********9012");
        assertThat(response.getBankAccountMasked()).doesNotContain("12345678");
        // EmartCardResponse has no panNumber field at all
    }

    @Test
    @DisplayName("asking for a card you never applied for gives a clear 404")
    void noCardGives404() {
        when(emartCardRepository.findByUser_UserId(1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> emartCardService.getMyCard())
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Apply first");
    }

    @Test
    @DisplayName("getMyCard returns only the caller's own card")
    void getMyCardIsScopedToOwner() {
        EmartCard card = EmartCard.builder()
                .cardId(5).user(me).cardNumber("EMCARD-1234567")
                .status(CardStatus.APPROVED).pointsBalance(350)
                .bankAccountNo("123456789012").panNumber("ABCDE1234F").build();
        when(emartCardRepository.findByUser_UserId(1)).thenReturn(Optional.of(card));

        EmartCardResponse response = emartCardService.getMyCard();

        assertThat(response.getPointsBalance()).isEqualTo(350);
        verify(emartCardRepository).findByUser_UserId(1);
    }

    private EmartCardApplicationRequest request() {
        return EmartCardApplicationRequest.builder()
                .employmentDetails("Software Engineer, Infosys")
                .bankAccountNo("123456789012")
                .panNumber("ABCDE1234F")
                .build();
    }
}
