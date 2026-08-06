using System.Collections.Generic;
using System.Threading.Tasks;
using Emart.Api.DTOs;
using Emart.Api.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace Emart.Api.Controllers
{
    [ApiController]
    [Route("api/[controller]")]
    [Authorize]
    public class WishlistController : ControllerBase
    {
        private readonly IWishlistService _wishlistService;

        public WishlistController(IWishlistService wishlistService)
        {
            _wishlistService = wishlistService;
        }

        [HttpGet]
        public async Task<ActionResult<ApiResponse<IEnumerable<WishlistDto>>>> GetMyWishlist()
        {
            var wishlist = await _wishlistService.GetMyWishlistAsync();
            return Ok(ApiResponse<IEnumerable<WishlistDto>>.CreateSuccess("Wishlist retrieved successfully", wishlist));
        }

        [HttpPost]
        public async Task<ActionResult<ApiResponse<WishlistDto>>> AddToWishlist([FromBody] WishlistRequest request)
        {
            var saved = await _wishlistService.AddToWishlistAsync(request);
            return StatusCode(201, ApiResponse<WishlistDto>.CreateSuccess("Product added to wishlist", saved));
        }

        [HttpDelete("{wishlistId}")]
        public async Task<ActionResult<ApiResponse<object>>> RemoveFromWishlist(int wishlistId)
        {
            await _wishlistService.RemoveFromWishlistAsync(wishlistId);
            return Ok(ApiResponse<object>.CreateSuccess("Product removed from wishlist"));
        }
    }
}
