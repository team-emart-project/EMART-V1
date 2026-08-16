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
    public class CategoryService : ICategoryService
    {
        private const string RootMarker = "^";
        private const int MaxDepth = 10;

        private readonly EmartDbContext _context;

        public CategoryService(EmartDbContext context)
        {
            _context = context;
        }

        public async Task<IEnumerable<CategoryDto>> GetRootCategoriesAsync()
        {
            var roots = await _context.CategoryMasters
                .AsNoTracking()
                .Where(c => c.SubcatId == RootMarker)
                .OrderBy(c => c.CatName)
                .ToListAsync();

            return roots.Select(CategoryMapper.ToDto).ToList();
        }

        /// <summary>
        /// The whole tree, assembled in ONE pass in memory rather than a query
        /// per level. The hierarchy uses the flat CHAR(3) codes: subcat_id = '^'
        /// means a root, otherwise it holds the PARENT's cat_id.
        /// </summary>
        public async Task<IEnumerable<CategoryDto>> GetCategoryTreeAsync()
        {
            var all = await _context.CategoryMasters.AsNoTracking().ToListAsync();

            var childrenByParentCode = all
                .Where(c => !string.IsNullOrWhiteSpace(c.SubcatId) && c.SubcatId != RootMarker)
                .GroupBy(c => c.SubcatId!.Trim().ToUpperInvariant())
                .ToDictionary(g => g.Key, g => g.ToList());

            return all
                .Where(IsRoot)
                .OrderBy(c => c.CatName)
                .Select(root => BuildNode(root, childrenByParentCode, new HashSet<int>(), 0))
                .ToList();
        }

        public async Task<CategoryDto?> GetCategoryAsync(int catmasterId)
        {
            var category = await _context.CategoryMasters
                .AsNoTracking()
                .FirstOrDefaultAsync(c => c.CatmasterId == catmasterId);

            return category == null ? null : CategoryMapper.ToDto(category);
        }

        public async Task<IEnumerable<CategoryDto>> GetSubCategoriesAsync(int catmasterId)
        {
            var parent = await _context.CategoryMasters
                .AsNoTracking()
                .FirstOrDefaultAsync(c => c.CatmasterId == catmasterId)
                ?? throw new ResourceNotFoundException("Category", "catmasterId", catmasterId);

            if (string.IsNullOrWhiteSpace(parent.CatId)) return new List<CategoryDto>();

            var subcats = await _context.CategoryMasters
                .AsNoTracking()
                .Where(c => c.SubcatId != null && c.SubcatId == parent.CatId)
                .OrderBy(c => c.CatName)
                .ToListAsync();

            return subcats.Select(CategoryMapper.ToDto).ToList();
        }

        private static bool IsRoot(CategoryMaster category) =>
            string.IsNullOrWhiteSpace(category.SubcatId) || category.SubcatId.Trim() == RootMarker;

        /// <summary>
        /// `visited` is copied per branch so a category legitimately reachable
        /// down two different paths still renders, while a genuine cycle is
        /// stopped the moment it revisits a node on its OWN path.
        /// </summary>
        private static CategoryDto BuildNode(CategoryMaster category,
                                             Dictionary<string, List<CategoryMaster>> childrenByParentCode,
                                             HashSet<int> visited,
                                             int depth)
        {
            var node = CategoryMapper.ToDto(category);

            if (depth >= MaxDepth || !visited.Add(category.CatmasterId))
            {
                node.Children = new List<CategoryDto>();
                return node;
            }

            var code = category.CatId?.Trim().ToUpperInvariant();
            var children = code != null && childrenByParentCode.TryGetValue(code, out var found)
                ? found
                : new List<CategoryMaster>();

            node.Children = children
                .OrderBy(c => c.CatName)
                .Select(child => BuildNode(child, childrenByParentCode, new HashSet<int>(visited), depth + 1))
                .ToList();

            return node;
        }
    }
}
