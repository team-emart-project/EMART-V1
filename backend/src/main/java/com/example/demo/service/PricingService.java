package com.example.demo.service;

import com.example.demo.entity.ProductMaster;
import com.example.demo.enums.PriceOption;
import com.example.demo.exception.BusinessRuleViolationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * The single place that turns a {@link PriceOption} into money and points.
 *
 * WHY THIS IS ITS OWN CLASS
 * -------------------------
 * Three different flows need the same answer: adding to the cart, previewing
 * the checkout, and placing the order. If each computed the price itself, the
 * preview could quietly disagree with what actually gets charged — the exact
 * class of bug a shopper notices and never forgives. One method, called from
 * all three, cannot disagree with itself.
 *
 * Nothing here trusts the client. The request carries only WHICH option was
 * ticked; the numbers are always re-read from the live catalogue row.
 */
@Service
public class PricingService {

    /** Can non-cardholders use the member options? Normally no. */
    @Value("${emart.cart.require-cardholder-for-points:true}")
    private boolean requireCardholder;

    /**
     * The resolved cost of ONE unit under a chosen option.
     *
     * A record because it is pure data with no identity — two resolutions with
     * the same cash and points are the same thing.
     */
    public record ResolvedPrice(BigDecimal cashPerUnit, int pointsPerUnit) {

        public BigDecimal cashFor(int quantity) {
            return cashPerUnit.multiply(BigDecimal.valueOf(quantity));
        }

        public int pointsFor(int quantity) {
            return pointsPerUnit * quantity;
        }
    }

    /**
     * Resolves one unit of {@code product} under {@code option}.
     *
     * Throws rather than silently falling back to the normal price: if the UI
     * offered an option the catalogue does not have, that is a bug worth
     * surfacing, not something to paper over by charging a different amount
     * than the shopper saw.
     */
    public ResolvedPrice resolve(ProductMaster product, PriceOption option) {
        PriceOption chosen = option == null ? PriceOption.REGULAR : option;

        return switch (chosen) {
            case REGULAR -> new ResolvedPrice(product.getMrpPrice(), 0);

            case MEMBER -> {
                requireOffer(product.hasMemberOffer(), product, "member price");
                yield new ResolvedPrice(product.getCardholderPrice(), 0);
            }

            // Cash is exactly zero here — the whole point of Option 2 is that
            // the shopper pays nothing but points.
            case POINTS -> {
                requireOffer(product.hasPointsOffer(), product, "points-only price");
                yield new ResolvedPrice(BigDecimal.ZERO, product.getPointsPrice());
            }

            case HYBRID -> {
                requireOffer(product.hasHybridOffer(), product, "combo price");
                yield new ResolvedPrice(product.getHybridCashPrice(), product.getHybridPoints());
            }
        };
    }

    /**
     * Full validation for putting {@code quantity} units on a line at
     * {@code option}, for a shopper who has {@code pointsAvailable} e-Points.
     *
     * Checks in the order a person would: are you allowed to use this option at
     * all, does the product offer it, and can you afford the points.
     */
    public void validate(ProductMaster product, PriceOption option, int quantity,
                         boolean isCardholder, int pointsAvailable) {

        PriceOption chosen = option == null ? PriceOption.REGULAR : option;

        if (chosen.requiresCardholder() && requireCardholder && !isCardholder) {
            throw new BusinessRuleViolationException(
                    "Only e-MART cardholders can use member pricing. Apply for a card to unlock it.");
        }

        // resolve() does the "is this offer actually on the product" check and
        // throws with a clear message if not, so we reuse it rather than
        // repeating the three null checks here.
        ResolvedPrice price = resolve(product, chosen);

        if (chosen.spendsPoints()) {
            int needed = price.pointsFor(quantity);
            if (needed > pointsAvailable) {
                throw new BusinessRuleViolationException(
                        "You don't have enough e-Points. This needs %d, you have %d."
                                .formatted(needed, pointsAvailable));
            }
        }
    }

    private void requireOffer(boolean available, ProductMaster product, String offerName) {
        if (!available) {
            throw new BusinessRuleViolationException(
                    "'%s' is not available at the %s.".formatted(product.getProdName(), offerName));
        }
    }
}
