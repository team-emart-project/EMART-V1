package com.example.demo.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaymentResponse {

    private Integer paymentId;
    private Integer orderId;
    private String orderNo;
    private String paymentMethod;
    private String cardLast4;
    private BigDecimal amount;
    private String status;
    private String transactionRef;
    private LocalDateTime transactionDate;

    /** Populated on a successful payment so the UI can show the new balance. */
    private Integer pointsEarned;
    private Integer pointsRedeemed;
    private Integer pointsBalanceAfter;
    private String orderStatus;

    public PaymentResponse() {
    }

    public PaymentResponse(Integer paymentId, Integer orderId, String orderNo, String paymentMethod,
                           String cardLast4, BigDecimal amount, String status, String transactionRef,
                           LocalDateTime transactionDate, Integer pointsEarned, Integer pointsRedeemed,
                           Integer pointsBalanceAfter, String orderStatus) {
        this.paymentId = paymentId;
        this.orderId = orderId;
        this.orderNo = orderNo;
        this.paymentMethod = paymentMethod;
        this.cardLast4 = cardLast4;
        this.amount = amount;
        this.status = status;
        this.transactionRef = transactionRef;
        this.transactionDate = transactionDate;
        this.pointsEarned = pointsEarned;
        this.pointsRedeemed = pointsRedeemed;
        this.pointsBalanceAfter = pointsBalanceAfter;
        this.orderStatus = orderStatus;
    }

    public Integer getPaymentId() { return paymentId; }
    public void setPaymentId(Integer v) { this.paymentId = v; }
    public Integer getOrderId() { return orderId; }
    public void setOrderId(Integer v) { this.orderId = v; }
    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String v) { this.orderNo = v; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String v) { this.paymentMethod = v; }
    public String getCardLast4() { return cardLast4; }
    public void setCardLast4(String v) { this.cardLast4 = v; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal v) { this.amount = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
    public String getTransactionRef() { return transactionRef; }
    public void setTransactionRef(String v) { this.transactionRef = v; }
    public LocalDateTime getTransactionDate() { return transactionDate; }
    public void setTransactionDate(LocalDateTime v) { this.transactionDate = v; }
    public Integer getPointsEarned() { return pointsEarned; }
    public void setPointsEarned(Integer v) { this.pointsEarned = v; }
    public Integer getPointsRedeemed() { return pointsRedeemed; }
    public void setPointsRedeemed(Integer v) { this.pointsRedeemed = v; }
    public Integer getPointsBalanceAfter() { return pointsBalanceAfter; }
    public void setPointsBalanceAfter(Integer v) { this.pointsBalanceAfter = v; }
    public String getOrderStatus() { return orderStatus; }
    public void setOrderStatus(String v) { this.orderStatus = v; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Integer paymentId;
        private Integer orderId;
        private String orderNo;
        private String paymentMethod;
        private String cardLast4;
        private BigDecimal amount;
        private String status;
        private String transactionRef;
        private LocalDateTime transactionDate;
        private Integer pointsEarned;
        private Integer pointsRedeemed;
        private Integer pointsBalanceAfter;
        private String orderStatus;

        public Builder paymentId(Integer v) { this.paymentId = v; return this; }
        public Builder orderId(Integer v) { this.orderId = v; return this; }
        public Builder orderNo(String v) { this.orderNo = v; return this; }
        public Builder paymentMethod(String v) { this.paymentMethod = v; return this; }
        public Builder cardLast4(String v) { this.cardLast4 = v; return this; }
        public Builder amount(BigDecimal v) { this.amount = v; return this; }
        public Builder status(String v) { this.status = v; return this; }
        public Builder transactionRef(String v) { this.transactionRef = v; return this; }
        public Builder transactionDate(LocalDateTime v) { this.transactionDate = v; return this; }
        public Builder pointsEarned(Integer v) { this.pointsEarned = v; return this; }
        public Builder pointsRedeemed(Integer v) { this.pointsRedeemed = v; return this; }
        public Builder pointsBalanceAfter(Integer v) { this.pointsBalanceAfter = v; return this; }
        public Builder orderStatus(String v) { this.orderStatus = v; return this; }

        public PaymentResponse build() {
            return new PaymentResponse(paymentId, orderId, orderNo, paymentMethod, cardLast4,
                    amount, status, transactionRef, transactionDate, pointsEarned, pointsRedeemed,
                    pointsBalanceAfter, orderStatus);
        }
    }
}
