package com.example.demo.entity;

import com.example.demo.enums.CardStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Maps the `emart_card` table — the loyalty card and its e-Points balance.
 *
 * emart_card.user_id is UNIQUE, so a user can hold at most one card.
 */
@Entity
@Table(name = "emart_card")
public class EmartCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "card_id")
    private Integer cardId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "card_number", nullable = false, unique = true, length = 30)
    private String cardNumber;

    @Column(name = "application_date", nullable = false)
    private LocalDate applicationDate;

    @Column(name = "approval_date")
    private LocalDate approvalDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CardStatus status;

    /**
     * Current e-Points balance. Only ever changed by Module 9 (Payment) after a
     * successful order — never by this module.
     */
    @Column(name = "points_balance", nullable = false)
    private Integer pointsBalance;

    @Column(name = "employment_details", length = 255)
    private String employmentDetails;

    @Column(name = "bank_account_no", length = 30)
    private String bankAccountNo;

    @Column(name = "pan_number", length = 20)
    private String panNumber;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public EmartCard() {
    }

    public EmartCard(Integer cardId, User user, String cardNumber, LocalDate applicationDate, LocalDate approvalDate, CardStatus status, Integer pointsBalance, String employmentDetails, String bankAccountNo, String panNumber, LocalDateTime createdAt) {
        this.cardId = cardId;
        this.user = user;
        this.cardNumber = cardNumber;
        this.applicationDate = applicationDate;
        this.approvalDate = approvalDate;
        this.status = status;
        this.pointsBalance = pointsBalance;
        this.employmentDetails = employmentDetails;
        this.bankAccountNo = bankAccountNo;
        this.panNumber = panNumber;
        this.createdAt = createdAt;
    }

    public Integer getCardId() {
        return cardId;
    }

    public void setCardId(Integer cardId) {
        this.cardId = cardId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
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

    public CardStatus getStatus() {
        return status;
    }

    public void setStatus(CardStatus status) {
        this.status = status;
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

    public String getBankAccountNo() {
        return bankAccountNo;
    }

    public void setBankAccountNo(String bankAccountNo) {
        this.bankAccountNo = bankAccountNo;
    }

    public String getPanNumber() {
        return panNumber;
    }

    public void setPanNumber(String panNumber) {
        this.panNumber = panNumber;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private Integer cardId;
        private User user;
        private String cardNumber;
        private LocalDate applicationDate;
        private LocalDate approvalDate;
        private CardStatus status;
        private Integer pointsBalance;
        private String employmentDetails;
        private String bankAccountNo;
        private String panNumber;
        private LocalDateTime createdAt;

        public Builder cardId(Integer cardId) {
            this.cardId = cardId;
            return this;
        }

        public Builder user(User user) {
            this.user = user;
            return this;
        }

        public Builder cardNumber(String cardNumber) {
            this.cardNumber = cardNumber;
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

        public Builder status(CardStatus status) {
            this.status = status;
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

        public Builder bankAccountNo(String bankAccountNo) {
            this.bankAccountNo = bankAccountNo;
            return this;
        }

        public Builder panNumber(String panNumber) {
            this.panNumber = panNumber;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public EmartCard build() {
            return new EmartCard(cardId, user, cardNumber, applicationDate, approvalDate, status, pointsBalance, employmentDetails, bankAccountNo, panNumber, createdAt);
        }

    }
}
