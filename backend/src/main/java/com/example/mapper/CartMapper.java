package com.example.mapper;

import com.example.dto.response.CartItemResponse;
import com.example.dto.response.CartResponse;
import com.example.entity.Cart;
import com.example.entity.CartItem;
import com.example.entity.ProductMaster;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Turns entities into response DTOs and calculates the cart totals.
 *
 * Written by hand rather than with MapStruct because this is not a straight
 * field copy — which price applies depends on whether the user is a cardholder,
 * and every total is derived. A generated mapper would not express that.
 */
@Component
public class CartMapper {

    /** The price this particular user pays for one unit. */
    public BigDecimal resolveUnitPrice(ProductMaster product, boolean cardholder) {
        return cardholder ? product.getCardholderPrice() : product.getMrpPrice();
    }

    public CartItemResponse toItemResponse(CartItem item, boolean cardholder) {
        ProductMaster product = item.getProduct();
        int quantity = item.getQuantity();

        BigDecimal unitPrice = resolveUnitPrice(product, cardholder);
        BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
        BigDecimal lineAtMrp = product.getMrpPrice().multiply(BigDecimal.valueOf(quantity));

        return CartItemResponse.builder()
                .cartItemId(item.getCartItemId())
                .prodId(product.getProdId())
                .prodName(product.getProdName())
                .prodImagePath(product.getProdImagePath())
                .mrpPrice(product.getMrpPrice())
                .cardholderPrice(product.getCardholderPrice())
                .unitPriceApplied(unitPrice)
                .quantity(quantity)
                .lineTotal(lineTotal)
                .lineSavings(lineAtMrp.subtract(lineTotal))
                .redeemPoints(item.getRedeemPoints())
                .pointsUsed(item.getPointsUsed())
                .maxPointsRedeemable(product.getPointsToRedeem() * quantity)
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
