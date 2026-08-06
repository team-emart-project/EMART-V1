using System;
using System.Linq;
using System.Threading.Tasks;
using Emart.Api.Data;
using Emart.Api.DTOs;
using Emart.Api.Mappings;
using Emart.Api.Middleware;
using Emart.Api.Models;
using Microsoft.EntityFrameworkCore;

namespace Emart.Api.Services
{
    public interface ICartService
    {
        Task<CartDto> GetCartAsync();
        Task<CartDto> AddItemAsync(CartItemRequest request);
        Task<CartDto> UpdateItemAsync(int cartItemId, UpdateCartItemRequest request);
        Task<CartDto> RemoveItemAsync(int cartItemId);
        Task<CartDto> ClearCartAsync();
    }

    public class CartService : ICartService
    {
        private readonly EmartDbContext _context;
        private readonly ISecurityUtils _securityUtils;
        private readonly ICardholderService _cardholderService;
        private readonly IPricingService _pricingService;
        private readonly ILogger<CartService> _logger;

        public CartService(EmartDbContext context,
                           ISecurityUtils securityUtils,
                           ICardholderService cardholderService,
                           IPricingService pricingService,
                           ILogger<CartService> logger)
        {
            _context = context;
            _securityUtils = securityUtils;
            _cardholderService = cardholderService;
            _pricingService = pricingService;
            _logger = logger;
        }

        public async Task<CartDto> GetCartAsync()
        {
            var user = await CurrentUserAsync();
            await GetOrCreateCartAsync(user);
            return await ReloadAsync(user);
        }

        public async Task<CartDto> AddItemAsync(CartItemRequest request)
        {
            var user = await CurrentUserAsync();
            var cart = await GetOrCreateCartAsync(user);

            var product = await _context.ProductMasters
                .FirstOrDefaultAsync(p => p.ProdId == request.ProdId)
                ?? throw new ResourceNotFoundException("Product", "prodId", request.ProdId);

            var existing = await _context.CartItems
                .FirstOrDefaultAsync(i => i.CartId == cart.CartId && i.ProdId == product.ProdId);

            if (existing != null)
            {
                // Same product already in the cart -> bump the quantity instead
                // of inserting a duplicate line. The newly ticked option wins,
                // because the shopper just told us what they want.
                int newQuantity = existing.Quantity + Math.Max(1, request.Quantity);
                await ApplyOptionAsync(user, existing, product, newQuantity, request.PriceOption);
            }
            else
            {
                var item = new CartItem { CartId = cart.CartId, ProdId = product.ProdId };
                await ApplyOptionAsync(user, item, product, Math.Max(1, request.Quantity), request.PriceOption);
                _context.CartItems.Add(item);
            }

            await _context.SaveChangesAsync();
            return await ReloadAsync(user);
        }

        public async Task<CartDto> UpdateItemAsync(int cartItemId, UpdateCartItemRequest request)
        {
            var user = await CurrentUserAsync();
            var item = await LoadOwnedItemAsync(cartItemId, user);

            var product = await _context.ProductMasters
                .FirstOrDefaultAsync(p => p.ProdId == item.ProdId)
                ?? throw new ResourceNotFoundException("Product", "prodId", item.ProdId);

            // A null priceOption means "just changing the quantity" — keep
            // whatever the line already had.
            var option = request.PriceOption ?? item.PriceOption;

            await ApplyOptionAsync(user, item, product, request.Quantity, option);
            await _context.SaveChangesAsync();

            return await ReloadAsync(user);
        }

        public async Task<CartDto> RemoveItemAsync(int cartItemId)
        {
            var user = await CurrentUserAsync();
            var item = await LoadOwnedItemAsync(cartItemId, user);

            _context.CartItems.Remove(item);
            await _context.SaveChangesAsync();

            return await ReloadAsync(user);
        }

        public async Task<CartDto> ClearCartAsync()
        {
            var user = await CurrentUserAsync();
            var cart = await GetOrCreateCartAsync(user);

            var items = await _context.CartItems.Where(i => i.CartId == cart.CartId).ToListAsync();
            _context.CartItems.RemoveRange(items);
            await _context.SaveChangesAsync();

            return await ReloadAsync(user);
        }

        // ------------------------------------------------------------------

        private async Task<User> CurrentUserAsync()
        {
            int userId = _securityUtils.GetCurrentUserId();
            return await _context.Users.FirstOrDefaultAsync(u => u.UserId == userId)
                   ?? throw new ResourceNotFoundException("User", "userId", userId);
        }

        private async Task<Cart> GetOrCreateCartAsync(User user)
        {
            var cart = await _context.Carts.FirstOrDefaultAsync(c => c.UserId == user.UserId);
            if (cart == null)
            {
                cart = new Cart { UserId = user.UserId, Status = CartStatus.ACTIVE };
                _context.Carts.Add(cart);
                await _context.SaveChangesAsync();
                _logger.LogDebug("Created first cart for userId={UserId}", user.UserId);
            }
            return cart;
        }

        private async Task<CartItem> LoadOwnedItemAsync(int cartItemId, User user)
        {
            var item = await _context.CartItems
                .Include(i => i.Cart)
                .FirstOrDefaultAsync(i => i.CartItemId == cartItemId)
                ?? throw new ResourceNotFoundException("Cart item", "cartItemId", cartItemId);

            if (item.Cart?.UserId != user.UserId)
            {
                throw new UnauthorizedActionException("This cart item does not belong to you");
            }
            return item;
        }

        /// <summary>
        /// Validates and re-prices a line from the LIVE product row. Both the
        /// cardholder check and the points balance are re-read every time,
        /// because either can change while a cart sits open.
        /// </summary>
        private async Task ApplyOptionAsync(User user, CartItem item, ProductMaster product,
                                            int quantity, PriceOption option)
        {
            bool cardholder = await _cardholderService.IsActiveCardholderAsync(user.UserId);
            int balance = await _cardholderService.GetPointsBalanceAsync(user.UserId);

            _pricingService.Validate(product, option, quantity, cardholder, balance);
            var price = _pricingService.Resolve(product, option);

            item.Quantity = quantity;
            item.PriceOption = option;
            item.PointsUsed = price.PointsFor(quantity);
        }

        private async Task<CartDto> ReloadAsync(User user)
        {
            var cart = await _context.Carts
                .AsNoTracking()
                .Include(c => c.Items)
                    .ThenInclude(i => i.Product)
                .FirstOrDefaultAsync(c => c.UserId == user.UserId)
                ?? throw new ResourceNotFoundException("Cart", "userId", user.UserId);

            bool cardholder = await _cardholderService.IsActiveCardholderAsync(user.UserId);
            return CartMapper.ToCartDto(cart, cardholder, _pricingService);
        }
    }
}
