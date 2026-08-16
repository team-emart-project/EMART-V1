package com.example.demo.dto.response;

import java.math.BigDecimal;
import java.util.List;

/**
 * The whole cart. Every total here is CALCULATED ON READ — none of it is
 * stored in the database, so it can never drift out of sync with the lines.
 */
public class CartResponse {

    private Integer cartId;
    private Integer userId;
    private String status;

    /** True when the user holds an approved e-MART card (drives pricing). */
    private Boolean cardholder;

    private List<CartItemResponse> items;

    /** Number of distinct products in the cart. */
    private Integer distinctItemCount;

    /** Sum of all quantities — the "N items" badge in the UI. */
    private Integer totalQuantity;

    /** What the cart would cost at full MRP. */
    private BigDecimal subtotalMrp;

    /** What the user actually pays. There is no tax in this project. */
    private BigDecimal subtotalPayable;

    /** subtotalMrp - subtotalPayable */
    private BigDecimal totalSavings;

    private Integer totalPointsUsed;

    public CartResponse() {
    }

    public CartResponse(Integer cartId, Integer userId, String status, Boolean cardholder,
                        List<CartItemResponse> items, Integer distinctItemCount, Integer totalQuantity,
                        BigDecimal subtotalMrp, BigDecimal subtotalPayable, BigDecimal totalSavings,
                        Integer totalPointsUsed) {
        this.cartId = cartId;
        this.userId = userId;
        this.status = status;
        this.cardholder = cardholder;
        this.items = items;
        this.distinctItemCount = distinctItemCount;
        this.totalQuantity = totalQuantity;
        this.subtotalMrp = subtotalMrp;
        this.subtotalPayable = subtotalPayable;
        this.totalSavings = totalSavings;
        this.totalPointsUsed = totalPointsUsed;
    }

    public Integer getCartId() {
        return cartId;
    }

    public void setCartId(Integer cartId) {
        this.cartId = cartId;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Boolean getCardholder() {
        return cardholder;
    }

    public void setCardholder(Boolean cardholder) {
        this.cardholder = cardholder;
    }

    public List<CartItemResponse> getItems() {
        return items;
    }

    public void setItems(List<CartItemResponse> items) {
        this.items = items;
    }

    public Integer getDistinctItemCount() {
        return distinctItemCount;
    }

    public void setDistinctItemCount(Integer distinctItemCount) {
        this.distinctItemCount = distinctItemCount;
    }

    public Integer getTotalQuantity() {
        return totalQuantity;
    }

    public void setTotalQuantity(Integer totalQuantity) {
        this.totalQuantity = totalQuantity;
    }

    public BigDecimal getSubtotalMrp() {
        return subtotalMrp;
    }

    public void setSubtotalMrp(BigDecimal subtotalMrp) {
        this.subtotalMrp = subtotalMrp;
    }

    public BigDecimal getSubtotalPayable() {
        return subtotalPayable;
    }

    public void setSubtotalPayable(BigDecimal subtotalPayable) {
        this.subtotalPayable = subtotalPayable;
    }

    public BigDecimal getTotalSavings() {
        return totalSavings;
    }

    public void setTotalSavings(BigDecimal totalSavings) {
        this.totalSavings = totalSavings;
    }

    public Integer getTotalPointsUsed() {
        return totalPointsUsed;
    }

    public void setTotalPointsUsed(Integer totalPointsUsed) {
        this.totalPointsUsed = totalPointsUsed;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private Integer cartId;
        private Integer userId;
        private String status;
        private Boolean cardholder;
        private List<CartItemResponse> items;
        private Integer distinctItemCount;
        private Integer totalQuantity;
        private BigDecimal subtotalMrp;
        private BigDecimal subtotalPayable;
        private BigDecimal totalSavings;
        private Integer totalPointsUsed;

        public Builder cartId(Integer cartId) {
            this.cartId = cartId;
            return this;
        }

        public Builder userId(Integer userId) {
            this.userId = userId;
            return this;
        }

        public Builder status(String status) {
            this.status = status;
            return this;
        }

        public Builder cardholder(Boolean cardholder) {
            this.cardholder = cardholder;
            return this;
        }

        public Builder items(List<CartItemResponse> items) {
            this.items = items;
            return this;
        }

        public Builder distinctItemCount(Integer distinctItemCount) {
            this.distinctItemCount = distinctItemCount;
            return this;
        }

        public Builder totalQuantity(Integer totalQuantity) {
            this.totalQuantity = totalQuantity;
            return this;
        }

        public Builder subtotalMrp(BigDecimal subtotalMrp) {
            this.subtotalMrp = subtotalMrp;
            return this;
        }

        public Builder subtotalPayable(BigDecimal subtotalPayable) {
            this.subtotalPayable = subtotalPayable;
            return this;
        }

        public Builder totalSavings(BigDecimal totalSavings) {
            this.totalSavings = totalSavings;
            return this;
        }

        public Builder totalPointsUsed(Integer totalPointsUsed) {
            this.totalPointsUsed = totalPointsUsed;
            return this;
        }

        public CartResponse build() {
            return new CartResponse(cartId, userId, status, cardholder, items, distinctItemCount,
                    totalQuantity, subtotalMrp, subtotalPayable, totalSavings, totalPointsUsed);
        }
    }
}
