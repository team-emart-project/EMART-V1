package com.example.demo.mapper;

import com.example.demo.dto.response.EmartCardResponse;
import com.example.demo.entity.EmartCard;
import org.springframework.stereotype.Component;

@Component
public class EmartCardMapper {

    public EmartCardResponse toResponse(EmartCard card) {
        return EmartCardResponse.builder()
                .cardId(card.getCardId())
                .cardNumber(card.getCardNumber())
                .status(card.getStatus() != null ? card.getStatus().name() : null)
                .applicationDate(card.getApplicationDate())
                .approvalDate(card.getApprovalDate())
                .pointsBalance(card.getPointsBalance())
                .employmentDetails(card.getEmploymentDetails())
                .bankAccountMasked(mask(card.getBankAccountNo()))
                .build();
        // panNumber is deliberately never returned.
    }

    /** Shows only the last 4 digits. */
    private String mask(String accountNumber) {
        if (accountNumber == null || accountNumber.length() < 4) {
            return null;
        }
        String last4 = accountNumber.substring(accountNumber.length() - 4);
        return "*".repeat(accountNumber.length() - 4) + last4;
    }
}
