using System;
using System.Collections.Generic;
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
    public interface IWishlistService
    {
        Task<IEnumerable<WishlistDto>> GetMyWishlistAsync();
        Task<WishlistDto> AddToWishlistAsync(WishlistRequest request);
        Task RemoveFromWishlistAsync(int wishlistId);
    }

    public class WishlistService : IWishlistService
    {
        private readonly EmartDbContext _context;
        private readonly ISecurityUtils _securityUtils;
        private readonly ICardholderService _cardholderService;

        public WishlistService(EmartDbContext context,
                               ISecurityUtils securityUtils,
                               ICardholderService cardholderService)
        {
            _context = context;
            _securityUtils = securityUtils;
            _cardholderService = cardholderService;
        }

        public async Task<IEnumerable<WishlistDto>> GetMyWishlistAsync()
        {
            int userId = _securityUtils.GetCurrentUserId();
            bool cardholder = await _cardholderService.IsActiveCardholderAsync(userId);

            var entries = await _context.Wishlists
                .AsNoTracking()
                .Include(w => w.Product)
                .Where(w => w.UserId == userId)
                .OrderByDescending(w => w.AddedAt)
                .ToListAsync();

            return entries.Select(w => WishlistMapper.ToDto(w, cardholder)).ToList();
        }

        public async Task<WishlistDto> AddToWishlistAsync(WishlistRequest request)
        {
            int userId = _securityUtils.GetCurrentUserId();

            var product = await _context.ProductMasters
                .FirstOrDefaultAsync(p => p.ProdId == request.ProdId)
                ?? throw new ResourceNotFoundException("Product", "prodId", request.ProdId);

            // (user_id, prod_id) is UNIQUE in the schema; checking first turns
            // what would be a raw constraint violation into a clean 409.
            if (await _context.Wishlists.AnyAsync(w => w.UserId == userId && w.ProdId == request.ProdId))
            {
                throw new DuplicateResourceException("This product is already in your wishlist");
            }

            var entry = new Wishlist
            {
                UserId = userId,
                ProdId = request.ProdId,
                AddedAt = DateTime.Now
            };

            _context.Wishlists.Add(entry);
            await _context.SaveChangesAsync();

            entry.Product = product;
            bool cardholder = await _cardholderService.IsActiveCardholderAsync(userId);
            return WishlistMapper.ToDto(entry, cardholder);
        }

        public async Task RemoveFromWishlistAsync(int wishlistId)
        {
            int userId = _securityUtils.GetCurrentUserId();

            var entry = await _context.Wishlists.FirstOrDefaultAsync(w => w.WishlistId == wishlistId)
                        ?? throw new ResourceNotFoundException("Wishlist entry", "wishlistId", wishlistId);

            if (entry.UserId != userId)
            {
                throw new UnauthorizedActionException("This wishlist entry does not belong to you");
            }

            _context.Wishlists.Remove(entry);
            await _context.SaveChangesAsync();
        }
    }
}
