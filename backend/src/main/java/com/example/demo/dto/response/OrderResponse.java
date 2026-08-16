package com.example.demo.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * A placed order, or a checkout PREVIEW of one.
 *
 * NO TAX. This project charges exactly the price shown on the product card:
 * totalAmount equals subtotalAmount. There is deliberately no tax field here,
 * on the order table, or on the invoice.
 *
 * e-Points are not modelled as a discount either. If a line was bought with
 * points, its cash price is simply lower (or zero) — the points are part of the
 * price, not something subtracted afterwards. pointsRedeemed records how many
 * were spent so the invoice can show it.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderResponse {

    /** Null on a preview — nothing was saved. */
    private Integer orderId;

    /** Null on a preview. */
    private String orderNo;

    private LocalDateTime orderDate;

    private String customerName;

    private String membershipNo;

    private Boolean cardholder;

    private AddressResponse shippingAddress;

    private AddressResponse billingAddress;

    private List<OrderDetailResponse> items;

    /** What the basket would cost at normal prices. */
    private BigDecimal subtotalMrp;

    /** Cash actually payable across all lines. */
    private BigDecimal subtotalAmount;

    /** subtotalMrp - subtotalAmount. */
    private BigDecimal totalSavings;

    /** Equals subtotalAmount. Kept as its own field so the UI and the payment tamper-check have one obvious number to read. */
    private BigDecimal totalAmount;

    private Integer pointsRedeemed;

    private Integer pointsEarned;

    /** Only populated by the payment response. */
    private Integer pointsBalanceAfter;

    private String paymentStatus;

    private String orderStatus;

    /** True when this was produced by /checkout-preview. */
    private Boolean preview;

    public OrderResponse() {
    }

    public OrderResponse(Integer orderId, String orderNo, LocalDateTime orderDate, String customerName,
                         String membershipNo, Boolean cardholder, AddressResponse shippingAddress,
                         AddressResponse billingAddress, List<OrderDetailResponse> items,
                         BigDecimal subtotalMrp, BigDecimal subtotalAmount, BigDecimal totalSavings,
                         BigDecimal totalAmount, Integer pointsRedeemed, Integer pointsEarned,
                         Integer pointsBalanceAfter, String paymentStatus, String orderStatus,
                         Boolean preview) {
        this.orderId = orderId;
        this.orderNo = orderNo;
        this.orderDate = orderDate;
        this.customerName = customerName;
        this.membershipNo = membershipNo;
        this.cardholder = cardholder;
        this.shippingAddress = shippingAddress;
        this.billingAddress = billingAddress;
        this.items = items;
        this.subtotalMrp = subtotalMrp;
        this.subtotalAmount = subtotalAmount;
        this.totalSavings = totalSavings;
        this.totalAmount = totalAmount;
        this.pointsRedeemed = pointsRedeemed;
        this.pointsEarned = pointsEarned;
        this.pointsBalanceAfter = pointsBalanceAfter;
        this.paymentStatus = paymentStatus;
        this.orderStatus = orderStatus;
        this.preview = preview;
    }

    public Integer getOrderId() { return orderId; }
    public void setOrderId(Integer orderId) { this.orderId = orderId; }

    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }

    public LocalDateTime getOrderDate() { return orderDate; }
    public void setOrderDate(LocalDateTime orderDate) { this.orderDate = orderDate; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getMembershipNo() { return membershipNo; }
    public void setMembershipNo(String membershipNo) { this.membershipNo = membershipNo; }

    public Boolean getCardholder() { return cardholder; }
    public void setCardholder(Boolean cardholder) { this.cardholder = cardholder; }

    public AddressResponse getShippingAddress() { return shippingAddress; }
    public void setShippingAddress(AddressResponse shippingAddress) { this.shippingAddress = shippingAddress; }

    public AddressResponse getBillingAddress() { return billingAddress; }
    public void setBillingAddress(AddressResponse billingAddress) { this.billingAddress = billingAddress; }

    public List<OrderDetailResponse> getItems() { return items; }
    public void setItems(List<OrderDetailResponse> items) { this.items = items; }

    public BigDecimal getSubtotalMrp() { return subtotalMrp; }
    public void setSubtotalMrp(BigDecimal subtotalMrp) { this.subtotalMrp = subtotalMrp; }

    public BigDecimal getSubtotalAmount() { return subtotalAmount; }
    public void setSubtotalAmount(BigDecimal subtotalAmount) { this.subtotalAmount = subtotalAmount; }

    public BigDecimal getTotalSavings() { return totalSavings; }
    public void setTotalSavings(BigDecimal totalSavings) { this.totalSavings = totalSavings; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public Integer getPointsRedeemed() { return pointsRedeemed; }
    public void setPointsRedeemed(Integer pointsRedeemed) { this.pointsRedeemed = pointsRedeemed; }

    public Integer getPointsEarned() { return pointsEarned; }
    public void setPointsEarned(Integer pointsEarned) { this.pointsEarned = pointsEarned; }

    public Integer getPointsBalanceAfter() { return pointsBalanceAfter; }
    public void setPointsBalanceAfter(Integer pointsBalanceAfter) { this.pointsBalanceAfter = pointsBalanceAfter; }

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

    public String getOrderStatus() { return orderStatus; }
    public void setOrderStatus(String orderStatus) { this.orderStatus = orderStatus; }

    public Boolean getPreview() { return preview; }
    public void setPreview(Boolean preview) { this.preview = preview; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private Integer orderId;
        private String orderNo;
        private LocalDateTime orderDate;
        private String customerName;
        private String membershipNo;
        private Boolean cardholder;
        private AddressResponse shippingAddress;
        private AddressResponse billingAddress;
        private List<OrderDetailResponse> items;
        private BigDecimal subtotalMrp;
        private BigDecimal subtotalAmount;
        private BigDecimal totalSavings;
        private BigDecimal totalAmount;
        private Integer pointsRedeemed;
        private Integer pointsEarned;
        private Integer pointsBalanceAfter;
        private String paymentStatus;
        private String orderStatus;
        private Boolean preview;

        public Builder orderId(Integer orderId) { this.orderId = orderId; return this; }
        public Builder orderNo(String orderNo) { this.orderNo = orderNo; return this; }
        public Builder orderDate(LocalDateTime orderDate) { this.orderDate = orderDate; return this; }
        public Builder customerName(String customerName) { this.customerName = customerName; return this; }
        public Builder membershipNo(String membershipNo) { this.membershipNo = membershipNo; return this; }
        public Builder cardholder(Boolean cardholder) { this.cardholder = cardholder; return this; }
        public Builder shippingAddress(AddressResponse shippingAddress) { this.shippingAddress = shippingAddress; return this; }
        public Builder billingAddress(AddressResponse billingAddress) { this.billingAddress = billingAddress; return this; }
        public Builder items(List<OrderDetailResponse> items) { this.items = items; return this; }
        public Builder subtotalMrp(BigDecimal subtotalMrp) { this.subtotalMrp = subtotalMrp; return this; }
        public Builder subtotalAmount(BigDecimal subtotalAmount) { this.subtotalAmount = subtotalAmount; return this; }
        public Builder totalSavings(BigDecimal totalSavings) { this.totalSavings = totalSavings; return this; }
        public Builder totalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; return this; }
        public Builder pointsRedeemed(Integer pointsRedeemed) { this.pointsRedeemed = pointsRedeemed; return this; }
        public Builder pointsEarned(Integer pointsEarned) { this.pointsEarned = pointsEarned; return this; }
        public Builder pointsBalanceAfter(Integer pointsBalanceAfter) { this.pointsBalanceAfter = pointsBalanceAfter; return this; }
        public Builder paymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; return this; }
        public Builder orderStatus(String orderStatus) { this.orderStatus = orderStatus; return this; }
        public Builder preview(Boolean preview) { this.preview = preview; return this; }

        public OrderResponse build() {
            return new OrderResponse(orderId, orderNo, orderDate, customerName, membershipNo, cardholder,
                    shippingAddress, billingAddress, items, subtotalMrp, subtotalAmount, totalSavings,
                    totalAmount, pointsRedeemed, pointsEarned, pointsBalanceAfter, paymentStatus,
                    orderStatus, preview);
        }
    }
}
