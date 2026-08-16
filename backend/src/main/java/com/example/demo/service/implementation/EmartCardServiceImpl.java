package com.example.demo.service.implementation;

import com.example.demo.dto.request.EmartCardApplicationRequest;
import com.example.demo.dto.response.EmartCardResponse;
import com.example.demo.dto.response.PointsBalanceResponse;
import com.example.demo.entity.EmartCard;
import com.example.demo.entity.User;
import com.example.demo.enums.CardStatus;
import com.example.demo.exception.DuplicateResourceException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.mapper.EmartCardMapper;
import com.example.demo.repository.EmartCardRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.SecurityUtils;
import com.example.demo.service.CardholderService;
import com.example.demo.service.interfaces.EmartCardService;
import com.example.demo.util.CardNumberGeneratorUtil;
import com.example.demo.util.EmailUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class EmartCardServiceImpl implements EmartCardService {

    private static final Logger log = LoggerFactory.getLogger(EmartCardServiceImpl.class);

    private final EmartCardRepository emartCardRepository;
    private final UserRepository userRepository;
    private final EmartCardMapper emartCardMapper;
    private final CardNumberGeneratorUtil cardNumberGenerator;
    private final EmailUtil emailUtil;
    private final SecurityUtils securityUtils;
    private final CardholderService cardholderService;

    /**
     * Approve card applications immediately.
     *
     * There is no admin module in this phase, so with this false a card would
     * sit at PENDING forever — which is exactly the bug that stopped e-Points
     * ever being credited. Set it to false only once an approval workflow exists.
     */
    @Value("${emart.card.auto-approve:true}")
    private boolean autoApprove;

    public EmartCardServiceImpl(EmartCardRepository emartCardRepository,
                                UserRepository userRepository,
                                EmartCardMapper emartCardMapper,
                                CardNumberGeneratorUtil cardNumberGenerator,
                                EmailUtil emailUtil,
                                SecurityUtils securityUtils,
                                CardholderService cardholderService) {
        this.emartCardRepository = emartCardRepository;
        this.userRepository = userRepository;
        this.emartCardMapper = emartCardMapper;
        this.cardNumberGenerator = cardNumberGenerator;
        this.emailUtil = emailUtil;
        this.securityUtils = securityUtils;
        this.cardholderService = cardholderService;
    }

    @Override
    @Transactional
    public EmartCardResponse apply(EmartCardApplicationRequest request) {

        User user = loadCurrentUser();

        if (emartCardRepository.existsByUser_UserId(user.getUserId())) {
            throw new DuplicateResourceException("You have already applied for an e-MART card");
        }

        EmartCard card = EmartCard.builder()
                .user(user)
                .cardNumber(cardNumberGenerator.generate())
                .applicationDate(LocalDate.now())
                .status(CardStatus.PENDING)
                .pointsBalance(0)
                .employmentDetails(request.getEmploymentDetails().trim())
                .bankAccountNo(request.getBankAccountNo().trim())
                .panNumber(request.getPanNumber().trim().toUpperCase())
                .build();

        EmartCard saved = emartCardRepository.save(card);

        if (autoApprove) {
            // BUG FIX: this is what was missing. Without it the card stayed
            // PENDING, users.is_cardholder stayed false, and every downstream
            // points check silently did nothing.
            cardholderService.approve(saved, LocalDate.now());
        }

        emailUtil.sendCardApplicationReceived(user.getEmail(), user.getFirstName());
        log.info("e-MART card {} for userId={} (status={})",
                saved.getCardNumber(), user.getUserId(), saved.getStatus());

        return emartCardMapper.toResponse(saved);
    }

    @Override
    public EmartCardResponse getMyCard() {
        Integer userId = securityUtils.getCurrentUserId();
        EmartCard card = emartCardRepository.findByUser_UserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "You do not have an e-MART card. Apply first at POST /api/emart-card/apply"));
        return emartCardMapper.toResponse(card);
    }

    @Override
    public PointsBalanceResponse getMyPointsBalance() {
        Integer userId = securityUtils.getCurrentUserId();
        Optional<EmartCard> card = cardholderService.findCard(userId);

        return PointsBalanceResponse.builder()
                .cardholder(card.map(c -> c.getStatus() == CardStatus.APPROVED).orElse(false))
                .pointsBalance(cardholderService.getPointsBalance(userId))
                .cardStatus(card.map(c -> c.getStatus().name()).orElse("NONE"))
                .build();
    }

    private User loadCurrentUser() {
        Integer userId = securityUtils.getCurrentUserId();
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "userId", userId));
    }
}
