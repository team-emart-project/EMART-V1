package com.example.demo.service.interfaces;

import com.example.demo.dto.request.WishlistRequest;
import com.example.demo.dto.response.WishlistResponse;

import java.util.List;

/** Module 7 — save-for-later list for the logged-in user. */
public interface WishlistService {

    List<WishlistResponse> getMyWishlist();

    WishlistResponse addToWishlist(WishlistRequest request);

    void removeFromWishlist(Integer wishlistId);
}
