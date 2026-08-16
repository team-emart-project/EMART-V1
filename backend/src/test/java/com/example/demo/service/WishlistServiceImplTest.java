package com.example.demo.service;

import com.example.demo.dto.request.WishlistRequest;
import com.example.demo.entity.CategoryMaster;
import com.example.demo.entity.ProductMaster;
import com.example.demo.entity.User;
import com.example.demo.entity.Wishlist;
import com.example.demo.exception.DuplicateResourceException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.exception.UnauthorizedActionException;
import com.example.demo.mapper.WishlistMapper;
import com.example.demo.repository.ProductMasterRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.WishlistRepository;
import com.example.demo.security.SecurityUtils;
import com.example.demo.service.implementation.WishlistServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WishlistServiceImplTest {

    @Mock private WishlistRepository wishlistRepository;
    @Mock private ProductMasterRepository productRepository;
    @Mock private UserRepository userRepository;
    @Mock private SecurityUtils securityUtils;
    @Spy  private WishlistMapper wishlistMapper = new WishlistMapper();

    @InjectMocks private WishlistServiceImpl wishlistService;

    private User me;
    private User someoneElse;
    private ProductMaster product;

    @BeforeEach
    void setUp() {
        me = User.builder().userId(1).email("me@example.com").isActive(true).build();
        someoneElse = User.builder().userId(2).email("other@example.com").isActive(true).build();

        product = ProductMaster.builder()
                .prodId(10)
                .category(CategoryMaster.builder().catmasterId(7).catName("Canon DSLR").build())
                .prodName("Canon EOS 1500D")
                .mrpPrice(new BigDecimal("32999.00"))
                .cardholderPrice(new BigDecimal("29999.00"))
                .build();

        when(securityUtils.getCurrentUserId()).thenReturn(1);
        when(userRepository.findById(1)).thenReturn(Optional.of(me));
        when(wishlistRepository.save(any(Wishlist.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("adding a product returns it with pricing")
    void addWorks() {
        when(productRepository.findById(10)).thenReturn(Optional.of(product));
        when(wishlistRepository.existsByUser_UserIdAndProduct_ProdId(1, 10)).thenReturn(false);

        var response = wishlistService.addToWishlist(WishlistRequest.builder().prodId(10).build());

        assertThat(response.getProdId()).isEqualTo(10);
        assertThat(response.getMrpPrice()).isEqualByComparingTo("32999.00");
        assertThat(response.getCardholderPrice()).isEqualByComparingTo("29999.00");
    }

    @Test
    @DisplayName("adding the same product twice is rejected")
    void duplicateRejected() {
        when(productRepository.findById(10)).thenReturn(Optional.of(product));
        when(wishlistRepository.existsByUser_UserIdAndProduct_ProdId(1, 10)).thenReturn(true);

        assertThatThrownBy(() -> wishlistService.addToWishlist(
                WishlistRequest.builder().prodId(10).build()))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already in your wishlist");

        verify(wishlistRepository, never()).save(any());
    }

    @Test
    @DisplayName("adding an unknown product gives a 404")
    void unknownProductRejected() {
        when(productRepository.findById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> wishlistService.addToWishlist(
                WishlistRequest.builder().prodId(999).build()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("a user cannot delete someone else's wishlist entry")
    void cannotDeleteForeignEntry() {
        Wishlist foreign = Wishlist.builder()
                .wishlistId(50).user(someoneElse).product(product).build();
        when(wishlistRepository.findById(50)).thenReturn(Optional.of(foreign));

        assertThatThrownBy(() -> wishlistService.removeFromWishlist(50))
                .isInstanceOf(UnauthorizedActionException.class);

        verify(wishlistRepository, never()).delete(any());
    }

    @Test
    @DisplayName("removing your own entry works")
    void removeOwnEntry() {
        Wishlist mine = Wishlist.builder().wishlistId(51).user(me).product(product).build();
        when(wishlistRepository.findById(51)).thenReturn(Optional.of(mine));

        wishlistService.removeFromWishlist(51);

        verify(wishlistRepository).delete(mine);
    }

    @Test
    @DisplayName("the list is scoped to the caller")
    void listIsScopedToOwner() {
        when(wishlistRepository.findByUserIdWithProduct(1)).thenReturn(List.of(
                Wishlist.builder().wishlistId(1).user(me).product(product).build()));

        assertThat(wishlistService.getMyWishlist()).hasSize(1);
        verify(wishlistRepository).findByUserIdWithProduct(1);
    }
}
