using System.Collections.Generic;
using System.Threading.Tasks;
using Emart.Api.DTOs;
using Emart.Api.Services;
using Microsoft.AspNetCore.Mvc;

namespace Emart.Api.Controllers
{
    [ApiController]
    [Route("api/[controller]")]
    public class CategoriesController : ControllerBase
    {
        private readonly ICategoryService _categoryService;
        private readonly IProductService _productService;

        public CategoriesController(ICategoryService categoryService, IProductService productService)
        {
            _categoryService = categoryService;
            _productService = productService;
        }

        [HttpGet]
        public async Task<ActionResult<ApiResponse<IEnumerable<CategoryDto>>>> GetCategories([FromQuery] bool flat = false)
        {
            var categories = flat 
                ? await _categoryService.GetRootCategoriesAsync()
                : await _categoryService.GetCategoryTreeAsync();

            return Ok(ApiResponse<IEnumerable<CategoryDto>>.CreateSuccess("Categories retrieved successfully", categories));
        }

        [HttpGet("{catmasterId}")]
        public async Task<ActionResult<ApiResponse<CategoryDto>>> GetCategory(int catmasterId)
        {
            var category = await _categoryService.GetCategoryAsync(catmasterId)
                ?? throw new Middleware.ResourceNotFoundException("Category", "catmasterId", catmasterId);

            return Ok(ApiResponse<CategoryDto>.CreateSuccess("Category retrieved successfully", category));
        }

        [HttpGet("{catmasterId}/subcategories")]
        public async Task<ActionResult<ApiResponse<IEnumerable<CategoryDto>>>> GetSubCategories(int catmasterId)
        {
            var subCats = await _categoryService.GetSubCategoriesAsync(catmasterId);
            return Ok(ApiResponse<IEnumerable<CategoryDto>>.CreateSuccess("Sub-categories retrieved successfully", subCats));
        }

        [HttpGet("{catmasterId}/products")]
        public async Task<ActionResult<ApiResponse<PageResponse<ProductDto>>>> GetProductsInCategory(
            int catmasterId, 
            [FromQuery] bool includeSubCategories = true, 
            [FromQuery] int page = 0, 
            [FromQuery] int size = 12)
        {
            var products = await _productService.GetProductsByCategoryAsync(catmasterId, includeSubCategories, page, size);
            return Ok(ApiResponse<PageResponse<ProductDto>>.CreateSuccess("Products retrieved successfully", products));
        }
    }
}
