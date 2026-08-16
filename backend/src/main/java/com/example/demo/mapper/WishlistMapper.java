package com.example.demo.mapper;

import com.example.demo.dto.response.WishlistResponse;
import com.example.demo.entity.ProductMaster;
import com.example.demo.entity.Wishlist;
import org.springframework.stereotype.Component;

@Component
public class WishlistMapper {

    /** Same price-visibility rule as ProductMapper: member price for members only. */
    public WishlistResponse toResponse(Wishlist wishlist, boolean isCardholder) {
        WishlistResponse response = toResponse(wishlist);
        if (!isCardholder) {
            response.setCardholderPrice(null);
        }
        return response;
    }

    public WishlistResponse toResponse(Wishlist wishlist) {
        ProductMaster p = wishlist.getProduct();
        return WishlistResponse.builder()
                .wishlistId(wishlist.getWishlistId())
                .prodId(p.getProdId())
                .prodName(p.getProdName())
                .prodShortDesc(p.getProdShortDesc())
                .prodImagePath(p.getProdImagePath())
                .mrpPrice(p.getMrpPrice())
                .cardholderPrice(p.getCardholderPrice())
                .pointsPrice(p.getPointsPrice())
                .addedAt(wishlist.getAddedAt())
                .build();
    }
}
