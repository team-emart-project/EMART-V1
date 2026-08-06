using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using Emart.Api.Data;
using Emart.Api.DTOs;
using Emart.Api.Mappings;
using Microsoft.EntityFrameworkCore;

namespace Emart.Api.Services
{
    public interface IHomeService
    {
        Task<IEnumerable<CategoryDto>> GetFeaturedCategoriesAsync();
        Task<IEnumerable<ProductDto>> GetNewArrivalsAsync(int limit);
    }

    public class HomeService : IHomeService
    {
        private const int MaxLimit = 50;

        private readonly EmartDbContext _context;
        private readonly ICardholderService _cardholderService;

        public HomeService(EmartDbContext context, ICardholderService cardholderService)
        {
            _context = context;
            _cardholderService = cardholderService;
        }

        /// <summary>Categories flagged flag = 1 — the round strip on the home page.</summary>
        public async Task<IEnumerable<CategoryDto>> GetFeaturedCategoriesAsync()
        {
            var featured = await _context.CategoryMasters
                .AsNoTracking()
                .Where(c => c.Flag)
                .OrderBy(c => c.CatName)
                .ToListAsync();

            return featured.Select(CategoryMapper.ToDto).ToList();
        }

        public async Task<IEnumerable<ProductDto>> GetNewArrivalsAsync(int limit)
        {
            int safeLimit = Math.Clamp(limit, 1, MaxLimit);

            var newArrivals = await _context.ProductMasters
                .AsNoTracking()
                .Include(p => p.Category)
                .OrderByDescending(p => p.CreatedAt)
                .ThenByDescending(p => p.ProdId)
                .Take(safeLimit)
                .ToListAsync();

            bool cardholder = await _cardholderService.IsCurrentUserCardholderAsync();
            return newArrivals.Select(p => ProductMapper.ToSummary(p, cardholder)).ToList();
        }
    }
}
