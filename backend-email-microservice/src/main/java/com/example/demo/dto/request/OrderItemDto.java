package com.example.demo.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * One invoice line. Every figure is the SNAPSHOT the backend stored when the
 * order was placed, so re-sending this email in a year prints the same numbers
 * the customer saw at checkout even if the catalogue has changed since.
 */
public record OrderItemDto(

        Integer prodId,

        @NotBlank(message = "Each item needs a product name")
        String prodName,

        @NotNull(message = "Each item needs a quantity")
        @Min(value = 1, message = "Quantity must be at least 1")
        Integer quantity,

        BigDecimal mrpPrice,

        /** Null when the product carried no member offer. */
        BigDecimal cardholderPrice,

        /** REGULAR / CARDHOLDER / POINTS / HYBRID — sent as a plain string so
         *  this service never has to keep an enum in step with two backends. */
        String priceOption,

        @NotNull(message = "Each item needs a charged price")
        BigDecimal priceCharged,

        BigDecimal lineTotal,

        BigDecimal lineSavings,

        Integer pointsRedeemed

) {
    /** priceCharged * quantity when the caller did not send it. */
    public BigDecimal lineTotalOrDerived() {
        if (lineTotal != null) return lineTotal;
        if (priceCharged == null || quantity == null) return BigDecimal.ZERO;
        return priceCharged.multiply(BigDecimal.valueOf(quantity));
    }

    public int pointsRedeemedOrZero() {
        return pointsRedeemed == null ? 0 : pointsRedeemed;
    }
}
