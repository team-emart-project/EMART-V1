using System.Collections.Generic;
using System.Threading.Tasks;
using Emart.Api.DTOs;
using Emart.Api.Services;
using Microsoft.AspNetCore.Mvc;

namespace Emart.Api.Controllers
{
    /// <summary>
    /// Public: the BRD says anyone may browse, an account is only needed to buy.
    /// These endpoints still read the JWT when one is present, because member
    /// pricing is only surfaced to an actual cardholder.
    /// </summary>
    [ApiController]
    [Route("api/products")]
    public class ProductsController : ControllerBase
    {
        private readonly IProductService _productService;

        public ProductsController(IProductService productService)
        {
            _productService = productService;
        }

        [HttpGet]
        public async Task<ActionResult<ApiResponse<PageResponse<ProductDto>>>> Search(
            [FromQuery] string? search = null,
            [FromQuery] decimal? minPrice = null,
            [FromQuery] decimal? maxPrice = null,
            [FromQuery] int page = 0,
            [FromQuery] int size = 12)
        {
            var products = await _productService.SearchProductsAsync(search, minPrice, maxPrice, page, size);
            return Ok(ApiResponse<PageResponse<ProductDto>>.CreateSuccess("Products retrieved successfully", products));
        }

        [HttpGet("{prodId:int}")]
        public async Task<ActionResult<ApiResponse<ProductDto>>> GetById(int prodId)
        {
            var product = await _productService.GetProductAsync(prodId);
            return Ok(ApiResponse<ProductDto>.CreateSuccess("Product retrieved successfully", product));
        }

        [HttpGet("{prodId:int}/variants")]
        public async Task<ActionResult<ApiResponse<List<ProductVariantDto>>>> GetVariants(int prodId)
        {
            var variants = await _productService.GetVariantsAsync(prodId);
            return Ok(ApiResponse<List<ProductVariantDto>>.CreateSuccess("Variants retrieved successfully", variants));
        }
    }
}
