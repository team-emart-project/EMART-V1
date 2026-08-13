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
    public class CartController : ControllerBase
    {
        private readonly ICartService _cartService;

        public CartController(ICartService cartService)
        {
            _cartService = cartService;
        }

        [HttpGet]
        public async Task<ActionResult<ApiResponse<CartDto>>> GetCart()
        {
            var cart = await _cartService.GetCartAsync();
            return Ok(ApiResponse<CartDto>.CreateSuccess("Cart retrieved successfully", cart));
        }

        [HttpPost("items")]
        public async Task<ActionResult<ApiResponse<CartDto>>> AddItem([FromBody] CartItemRequest request)
        {
            var cart = await _cartService.AddItemAsync(request);
            return StatusCode(201, ApiResponse<CartDto>.CreateSuccess("Item added to cart", cart));
        }

        [HttpPut("items/{cartItemId}")]
        public async Task<ActionResult<ApiResponse<CartDto>>> UpdateItem(int cartItemId, [FromBody] UpdateCartItemRequest request)
        {
            var cart = await _cartService.UpdateItemAsync(cartItemId, request);
            return Ok(ApiResponse<CartDto>.CreateSuccess("Cart item updated", cart));
        }

        [HttpDelete("items/{cartItemId}")]
        public async Task<ActionResult<ApiResponse<CartDto>>> RemoveItem(int cartItemId)
        {
            var cart = await _cartService.RemoveItemAsync(cartItemId);
            return Ok(ApiResponse<CartDto>.CreateSuccess("Cart item removed", cart));
        }

        [HttpDelete]
        public async Task<ActionResult<ApiResponse<CartDto>>> ClearCart()
        {
            var cart = await _cartService.ClearCartAsync();
            return Ok(ApiResponse<CartDto>.CreateSuccess("Cart cleared", cart));
        }
    }
}
