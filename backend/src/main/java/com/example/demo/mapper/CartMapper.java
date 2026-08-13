package com.example.demo.mapper;

import com.example.demo.dto.response.CartItemResponse;
import com.example.demo.dto.response.CartResponse;
import com.example.demo.entity.Cart;
import com.example.demo.entity.CartItem;
import com.example.demo.entity.ProductMaster;
import com.example.demo.enums.PriceOption;
import com.example.demo.service.PricingService;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Turns cart entities into response DTOs and calculates the totals.
 *
 * Written by hand rather than with MapStruct because this is not a straight
 * field copy — the price depends on the option the shopper ticked, and every
 * total is derived. A generated mapper could not express that.
 *
 * The mapper does NOT decide prices itself: it asks {@link PricingService},
 * the same class the cart and checkout use. That is deliberate — a mapper with
 * its own pricing rule is a second source of truth waiting to drift.
 */
@Component
public class CartMapper {

    private final PricingService pricingService;

    public CartMapper(PricingService pricingService) {
        this.pricingService = pricingService;
    }

    public CartItemResponse toItemResponse(CartItem item, boolean cardholder) {
        ProductMaster product = item.getProduct();
        int quantity = item.getQuantity();
        PriceOption option = item.getPriceOption() == null ? PriceOption.REGULAR : item.getPriceOption();

        PricingService.ResolvedPrice price = pricingService.resolve(product, option);

        BigDecimal lineTotal = price.cashFor(quantity);
        BigDecimal lineAtMrp = product.getMrpPrice().multiply(BigDecimal.valueOf(quantity));

        return CartItemResponse.builder()
                .cartItemId(item.getCartItemId())
                .prodId(product.getProdId())
                .prodName(product.getProdName())
                .prodImagePath(product.getProdImagePath())
                .mrpPrice(product.getMrpPrice())
                // The three offers are echoed back so the cart can render a
                // switcher. Only surfaced to an actual cardholder; NON_NULL
                // drops them from the JSON otherwise, exactly as on the
                // product listing.
                .cardholderPrice(cardholder ? product.getCardholderPrice() : null)
                .pointsPrice(cardholder ? product.getPointsPrice() : null)
                .hybridCashPrice(cardholder ? product.getHybridCashPrice() : null)
                .hybridPoints(cardholder ? product.getHybridPoints() : null)
                .priceOption(option)
                .unitPriceApplied(price.cashPerUnit())
                .unitPointsApplied(price.pointsPerUnit())
                .quantity(quantity)
                .lineTotal(lineTotal)
                .lineSavings(lineAtMrp.subtract(lineTotal))
                .pointsUsed(price.pointsFor(quantity))
                .build();
    }

    public CartResponse toCartResponse(Cart cart, boolean cardholder) {

        List<CartItemResponse> itemResponses = cart.getItems().stream()
                .map(item -> toItemResponse(item, cardholder))
                .toList();

        BigDecimal subtotalPayable = itemResponses.stream()
                .map(CartItemResponse::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal subtotalMrp = itemResponses.stream()
                .map(i -> i.getMrpPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int totalQuantity = itemResponses.stream()
                .mapToInt(CartItemResponse::getQuantity)
                .sum();

        int totalPointsUsed = itemResponses.stream()
                .mapToInt(CartItemResponse::getPointsUsed)
                .sum();

        return CartResponse.builder()
                .cartId(cart.getCartId())
                .userId(cart.getUser().getUserId())
                .status(cart.getStatus().name())
                .cardholder(cardholder)
                .items(itemResponses)
                .distinctItemCount(itemResponses.size())
                .totalQuantity(totalQuantity)
                .subtotalMrp(subtotalMrp)
                .subtotalPayable(subtotalPayable)
                .totalSavings(subtotalMrp.subtract(subtotalPayable))
                .totalPointsUsed(totalPointsUsed)
                .build();
    }
}
