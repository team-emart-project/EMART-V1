package com.example.demo.entity;

import com.example.demo.enums.PaymentStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Maps the `payment` table — one row per payment ATTEMPT.
 *
 * Failed attempts are kept, not overwritten, so the history of a customer
 * retrying a card is auditable.
 */
@Entity
@Table(name = "payment")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Integer paymentId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Orders order;

    @Column(name = "payment_method", nullable = false, length = 30)
    private String paymentMethod;

    /** Only the last 4 digits are ever stored. Never the full card number. */
    @Column(name = "card_last4", length = 4)
    private String cardLast4;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PaymentStatus status;

    @Column(name = "transaction_ref", length = 100)
    private String transactionRef;

    @CreationTimestamp
    @Column(name = "transaction_date", nullable = false, updatable = false)
    private LocalDateTime transactionDate;

    public Payment() {
    }

    public Payment(Integer paymentId, Orders order, String paymentMethod, String cardLast4,
                   BigDecimal amount, PaymentStatus status, String transactionRef,
                   LocalDateTime transactionDate) {
        this.paymentId = paymentId;
        this.order = order;
        this.paymentMethod = paymentMethod;
        this.cardLast4 = cardLast4;
        this.amount = amount;
        this.status = status;
        this.transactionRef = transactionRef;
        this.transactionDate = transactionDate;
    }

    public Integer getPaymentId() { return paymentId; }
    public void setPaymentId(Integer paymentId) { this.paymentId = paymentId; }

    public Orders getOrder() { return order; }
    public void setOrder(Orders order) { this.order = order; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getCardLast4() { return cardLast4; }
    public void setCardLast4(String cardLast4) { this.cardLast4 = cardLast4; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public PaymentStatus getStatus() { return status; }
    public void setStatus(PaymentStatus status) { this.status = status; }

    public String getTransactionRef() { return transactionRef; }
    public void setTransactionRef(String transactionRef) { this.transactionRef = transactionRef; }

    public LocalDateTime getTransactionDate() { return transactionDate; }
    public void setTransactionDate(LocalDateTime transactionDate) { this.transactionDate = transactionDate; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Integer paymentId;
        private Orders order;
        private String paymentMethod;
        private String cardLast4;
        private BigDecimal amount;
        private PaymentStatus status;
        private String transactionRef;
        private LocalDateTime transactionDate;

        public Builder paymentId(Integer paymentId) { this.paymentId = paymentId; return this; }
        public Builder order(Orders order) { this.order = order; return this; }
        public Builder paymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; return this; }
        public Builder cardLast4(String cardLast4) { this.cardLast4 = cardLast4; return this; }
        public Builder amount(BigDecimal amount) { this.amount = amount; return this; }
        public Builder status(PaymentStatus status) { this.status = status; return this; }
        public Builder transactionRef(String transactionRef) { this.transactionRef = transactionRef; return this; }
        public Builder transactionDate(LocalDateTime transactionDate) { this.transactionDate = transactionDate; return this; }

        public Payment build() {
            return new Payment(paymentId, order, paymentMethod, cardLast4, amount, status,
                    transactionRef, transactionDate);
        }
    }
}
