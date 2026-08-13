package com.example.demo.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDate;

/**
 * The card as the API exposes it.
 *
 * bankAccountNo is MASKED and panNumber is omitted entirely — echoing back
 * full financial identifiers is exactly the kind of leak DTOs exist to prevent.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EmartCardResponse {

    private Integer cardId;
    private String cardNumber;
    private String status;
    private LocalDate applicationDate;
    private LocalDate approvalDate;
    private Integer pointsBalance;
    private String employmentDetails;

    /** e.g. "******7890" */
    private String bankAccountMasked;

    public EmartCardResponse() {
    }

    public EmartCardResponse(Integer cardId, String cardNumber, String status, LocalDate applicationDate, LocalDate approvalDate, Integer pointsBalance, String employmentDetails, String bankAccountMasked) {
        this.cardId = cardId;
        this.cardNumber = cardNumber;
        this.status = status;
        this.applicationDate = applicationDate;
        this.approvalDate = approvalDate;
        this.pointsBalance = pointsBalance;
        this.employmentDetails = employmentDetails;
        this.bankAccountMasked = bankAccountMasked;
    }

    public Integer getCardId() {
        return cardId;
    }

    public void setCardId(Integer cardId) {
        this.cardId = cardId;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDate getApplicationDate() {
        return applicationDate;
    }

    public void setApplicationDate(LocalDate applicationDate) {
        this.applicationDate = applicationDate;
    }

    public LocalDate getApprovalDate() {
        return approvalDate;
    }

    public void setApprovalDate(LocalDate approvalDate) {
        this.approvalDate = approvalDate;
    }

    public Integer getPointsBalance() {
        return pointsBalance;
    }

    public void setPointsBalance(Integer pointsBalance) {
        this.pointsBalance = pointsBalance;
    }

    public String getEmploymentDetails() {
        return employmentDetails;
    }

    public void setEmploymentDetails(String employmentDetails) {
        this.employmentDetails = employmentDetails;
    }

    public String getBankAccountMasked() {
        return bankAccountMasked;
    }

    public void setBankAccountMasked(String bankAccountMasked) {
        this.bankAccountMasked = bankAccountMasked;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private Integer cardId;
        private String cardNumber;
        private String status;
        private LocalDate applicationDate;
        private LocalDate approvalDate;
        private Integer pointsBalance;
        private String employmentDetails;
        private String bankAccountMasked;

        public Builder cardId(Integer cardId) {
            this.cardId = cardId;
            return this;
        }

        public Builder cardNumber(String cardNumber) {
            this.cardNumber = cardNumber;
            return this;
        }

        public Builder status(String status) {
            this.status = status;
            return this;
        }

        public Builder applicationDate(LocalDate applicationDate) {
            this.applicationDate = applicationDate;
            return this;
        }

        public Builder approvalDate(LocalDate approvalDate) {
            this.approvalDate = approvalDate;
            return this;
        }

        public Builder pointsBalance(Integer pointsBalance) {
            this.pointsBalance = pointsBalance;
            return this;
        }

        public Builder employmentDetails(String employmentDetails) {
            this.employmentDetails = employmentDetails;
            return this;
        }

        public Builder bankAccountMasked(String bankAccountMasked) {
            this.bankAccountMasked = bankAccountMasked;
            return this;
        }

        public EmartCardResponse build() {
            return new EmartCardResponse(cardId, cardNumber, status, applicationDate, approvalDate, pointsBalance, employmentDetails, bankAccountMasked);
        }

    }
}
