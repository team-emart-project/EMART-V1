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
    public interface IProductService
    {
        Task<PageResponse<ProductDto>> SearchProductsAsync(string? search, decimal? minPrice, decimal? maxPrice, int page, int size);
        Task<PageResponse<ProductDto>> GetProductsByCategoryAsync(int catmasterId, bool includeSubCategories, int page, int size);
        Task<ProductDto> GetProductAsync(int prodId);
        Task<List<ProductVariantDto>> GetVariantsAsync(int prodId);
    }

    public class ProductService : IProductService
    {
        private const string RootMarker = "^";
        private const int MaxDepth = 10;

        private readonly EmartDbContext _context;
        private readonly ICardholderService _cardholderService;

        public ProductService(EmartDbContext context, ICardholderService cardholderService)
        {
            _context = context;
            _cardholderService = cardholderService;
        }

        public async Task<PageResponse<ProductDto>> SearchProductsAsync(
            string? search, decimal? minPrice, decimal? maxPrice, int page, int size)
        {
            if (minPrice.HasValue && maxPrice.HasValue && minPrice > maxPrice)
            {
                throw new BusinessRuleViolationException("minPrice cannot be greater than maxPrice");
            }

            IQueryable<ProductMaster> query = _context.ProductMasters
                .AsNoTracking()
                .Include(p => p.Category);

            // A blank search means "no filter", not "match the empty string".
            if (!string.IsNullOrWhiteSpace(search))
            {
                string term = search.Trim();
                query = query.Where(p => EF.Functions.Like(p.ProdName, $"%{term}%")
                                      || (p.Brand != null && EF.Functions.Like(p.Brand, $"%{term}%")));
            }
            if (minPrice.HasValue) query = query.Where(p => p.MrpPrice >= minPrice.Value);
            if (maxPrice.HasValue) query = query.Where(p => p.MrpPrice <= maxPrice.Value);

            return await PageAsync(query.OrderBy(p => p.ProdName), page, size);
        }

        public async Task<PageResponse<ProductDto>> GetProductsByCategoryAsync(
            int catmasterId, bool includeSubCategories, int page, int size)
        {
            var category = await _context.CategoryMasters.AsNoTracking()
                .FirstOrDefaultAsync(c => c.CatmasterId == catmasterId)
                ?? throw new ResourceNotFoundException("Category", "catmasterId", catmasterId);

            IQueryable<ProductMaster> query = _context.ProductMasters
                .AsNoTracking()
                .Include(p => p.Category);

            if (includeSubCategories)
            {
                // Products are filed against LEAF categories, so asking for
                // "Mobiles" without walking the branch returns nothing at all.
                var branchIds = await CollectBranchIdsAsync(category);
                query = query.Where(p => branchIds.Contains(p.CatmasterId));
            }
            else
            {
                query = query.Where(p => p.CatmasterId == catmasterId);
            }

            return await PageAsync(query.OrderBy(p => p.ProdName), page, size);
        }

        public async Task<ProductDto> GetProductAsync(int prodId)
        {
            var product = await _context.ProductMasters
                .AsNoTracking()
                .Include(p => p.Category)
                .FirstOrDefaultAsync(p => p.ProdId == prodId)
                ?? throw new ResourceNotFoundException("Product", "prodId", prodId);

            var images = await _context.ProductImages
                .AsNoTracking()
                .Where(i => i.ProdId == prodId)
                .OrderByDescending(i => i.IsPrimary)
                .ThenBy(i => i.DisplayOrder)
                .ToListAsync();

            var variants = await LoadVariantsAsync(prodId);
            bool cardholder = await _cardholderService.IsCurrentUserCardholderAsync();

            return ProductMapper.ToDetail(product, variants, images, cardholder);
        }

        public async Task<List<ProductVariantDto>> GetVariantsAsync(int prodId)
        {
            bool exists = await _context.ProductMasters.AnyAsync(p => p.ProdId == prodId);
            if (!exists) throw new ResourceNotFoundException("Product", "prodId", prodId);

            return await LoadVariantsAsync(prodId);
        }

        // ------------------------------------------------------------------

        private async Task<List<ProductVariantDto>> LoadVariantsAsync(int prodId)
        {
            var details = await _context.ProdDtlMasters
                .AsNoTracking()
                .Include(d => d.Config)
                .Where(d => d.ProdId == prodId)
                .OrderBy(d => d.ConfigId)
                .ThenBy(d => d.ProdDtlId)
                .ToListAsync();

            return ProductMapper.GroupVariants(details);
        }

        private async Task<PageResponse<ProductDto>> PageAsync(
            IQueryable<ProductMaster> query, int page, int size)
        {
            page = Math.Max(0, page);
            size = Math.Clamp(size, 1, 100);

            int totalElements = await query.CountAsync();
            var products = await query.Skip(page * size).Take(size).ToListAsync();

            bool cardholder = await _cardholderService.IsCurrentUserCardholderAsync();
            var content = products.Select(p => ProductMapper.ToSummary(p, cardholder)).ToList();

            int totalPages = (int)Math.Ceiling(totalElements / (double)size);
            return new PageResponse<ProductDto>(
                content, page, size, totalElements, totalPages,
                page == 0, page >= totalPages - 1);
        }

        /// <summary>
        /// Walks the flat CHAR(3) hierarchy: subcat_id holds the PARENT's
        /// cat_id, and '^' marks a root. Depth-capped and visited-tracked, so a
        /// data-entry cycle cannot spin here forever.
        /// </summary>
        private async Task<List<int>> CollectBranchIdsAsync(CategoryMaster root)
        {
            var all = await _context.CategoryMasters.AsNoTracking().ToListAsync();

            var childrenByParentCode = all
                .Where(c => !string.IsNullOrWhiteSpace(c.SubcatId) && c.SubcatId != RootMarker)
                .GroupBy(c => c.SubcatId!.Trim().ToUpperInvariant())
                .ToDictionary(g => g.Key, g => g.ToList());

            var ids = new HashSet<int>();
            var stack = new Stack<(CategoryMaster Category, int Depth)>();
            stack.Push((root, 0));

            while (stack.Count > 0)
            {
                var (current, depth) = stack.Pop();
                if (!ids.Add(current.CatmasterId) || depth >= MaxDepth) continue;

                var code = current.CatId?.Trim().ToUpperInvariant();
                if (code != null && childrenByParentCode.TryGetValue(code, out var children))
                {
                    foreach (var child in children) stack.Push((child, depth + 1));
                }
            }

            return ids.ToList();
        }
    }
}
