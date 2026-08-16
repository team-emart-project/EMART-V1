package com.example.demo.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * The invoice, exactly as the backend already computed it.
 *
 * NO TAX FIELD, deliberately: this project charges the price shown on the
 * product card, so totalAmount equals subtotalAmount. Adding a tax row to the
 * email would invent a number the order table does not have.
 *
 * e-Points are not a discount either. A line bought with points simply has a
 * lower cash price; pointsRedeemed only records how many were spent.
 */
public record OrderInvoiceDto(

        Integer orderId,

        @NotBlank(message = "orderNo is required")
        String orderNo,

        /**
         * ISO local date-time, e.g. 2026-08-10T14:22:31 — no offset. Both
         * callers send local time because the whole system runs in one
         * timezone; the .NET client formats it explicitly so a machine set to
         * a non-UTC kind cannot append "+05:30" and break parsing here.
         */
        LocalDateTime orderDate,

        /** PLACED / CONFIRMED / SHIPPED / DELIVERED / CANCELLED. */
        String orderStatus,

        /** PENDING at the moment an order is placed; PAID after payment. */
        String paymentStatus,

        /** What the basket would have cost at MRP. Drives "You saved". */
        BigDecimal subtotalMrp,

        @NotNull(message = "subtotalAmount is required")
        BigDecimal subtotalAmount,

        BigDecimal totalSavings,

        @NotNull(message = "totalAmount is required")
        BigDecimal totalAmount,

        Integer pointsRedeemed,

        Integer pointsEarned,

        // @Valid goes on the ELEMENT type, not the list. On the list itself it
        // still works but Hibernate Validator deprecates it, and here it also
        // reads better: it is each item that gets validated.
        @NotEmpty(message = "An order must have at least one item")
        List<@Valid OrderItemDto> items,

        @Valid AddressDto shippingAddress,

        @Valid AddressDto billingAddress

) {
    public LocalDateTime orderDateOrNow() {
        return orderDate == null ? LocalDateTime.now() : orderDate;
    }

    public int pointsRedeemedOrZero() {
        return pointsRedeemed == null ? 0 : pointsRedeemed;
    }

    public int pointsEarnedOrZero() {
        return pointsEarned == null ? 0 : pointsEarned;
    }

    /**
     * Falls back to subtotalMrp - subtotalAmount when the caller left it out,
     * so an older client that does not send the field still gets the savings
     * line instead of a blank one.
     */
    public BigDecimal totalSavingsOrDerived() {
        if (totalSavings != null) return totalSavings;
        if (subtotalMrp == null || subtotalAmount == null) return BigDecimal.ZERO;
        return subtotalMrp.subtract(subtotalAmount);
    }
}
