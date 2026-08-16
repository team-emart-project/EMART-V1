using System.Collections.Generic;
using System.Threading.Tasks;
using Emart.Api.DTOs;
using Emart.Api.Services;
using Microsoft.AspNetCore.Mvc;

namespace Emart.Api.Controllers
{
    [ApiController]
    [Route("api/[controller]")]
    public class HomeController : ControllerBase
    {
        private readonly IHomeService _homeService;

        public HomeController(IHomeService homeService)
        {
            _homeService = homeService;
        }

        [HttpGet("featured-categories")]
        public async Task<ActionResult<ApiResponse<IEnumerable<CategoryDto>>>> GetFeaturedCategories()
        {
            var featured = await _homeService.GetFeaturedCategoriesAsync();
            return Ok(ApiResponse<IEnumerable<CategoryDto>>.CreateSuccess("Featured categories retrieved successfully", featured));
        }

        [HttpGet("new-arrivals")]
        public async Task<ActionResult<ApiResponse<IEnumerable<ProductDto>>>> GetNewArrivals([FromQuery] int limit = 8)
        {
            var newArrivals = await _homeService.GetNewArrivalsAsync(limit);
            return Ok(ApiResponse<IEnumerable<ProductDto>>.CreateSuccess("New arrivals retrieved successfully", newArrivals));
        }
    }
}
