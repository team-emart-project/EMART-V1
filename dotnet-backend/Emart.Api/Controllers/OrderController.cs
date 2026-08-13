using System.Threading.Tasks;
using Emart.Api.DTOs;
using Emart.Api.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace Emart.Api.Controllers
{
    // The route is spelled out rather than taken from [controller]: the class is
    // OrderController, which would have produced /api/order, while the client
    // and the API reference both use /api/orders.
    [ApiController]
    [Route("api/orders")]
    [Authorize]
    public class OrderController : ControllerBase
    {
        private readonly IOrderService _orderService;

        public OrderController(IOrderService orderService)
        {
            _orderService = orderService;
        }

        [HttpPost("checkout-preview")]
        public async Task<ActionResult<ApiResponse<OrderDto>>> CheckoutPreview([FromBody] CheckoutRequest request)
        {
            var preview = await _orderService.CheckoutPreviewAsync(request);
            return Ok(ApiResponse<OrderDto>.CreateSuccess("Checkout preview calculated", preview));
        }

        [HttpPost]
        public async Task<ActionResult<ApiResponse<OrderDto>>> PlaceOrder([FromBody] CheckoutRequest request)
        {
            var order = await _orderService.PlaceOrderAsync(request);
            return StatusCode(201, ApiResponse<OrderDto>.CreateSuccess(
                "Order placed successfully. Proceed to payment.", order));
        }

        [HttpGet]
        public async Task<ActionResult<ApiResponse<PageResponse<OrderDto>>>> GetMyOrders(
            [FromQuery] int page = 0, [FromQuery] int size = 10)
        {
            var orders = await _orderService.GetMyOrdersAsync(page, size);
            return Ok(ApiResponse<PageResponse<OrderDto>>.CreateSuccess("Orders retrieved successfully", orders));
        }

        [HttpGet("{orderId:int}")]
        public async Task<ActionResult<ApiResponse<OrderDto>>> GetOrder(int orderId)
        {
            var order = await _orderService.GetOrderAsync(orderId);
            return Ok(ApiResponse<OrderDto>.CreateSuccess("Order retrieved successfully", order));
        }

        [HttpPut("{orderId:int}/cancel")]
        public async Task<ActionResult<ApiResponse<OrderDto>>> CancelOrder(int orderId)
        {
            var order = await _orderService.CancelOrderAsync(orderId);
            return Ok(ApiResponse<OrderDto>.CreateSuccess("Order cancelled", order));
        }

        /// <summary>
        /// The ONE endpoint that does not return the JSON envelope: the browser
        /// needs raw PDF bytes to render or download the file.
        /// </summary>
        [HttpGet("{orderId:int}/invoice-pdf")]
        public async Task<IActionResult> DownloadInvoice(int orderId)
        {
            var pdf = await _orderService.GenerateInvoicePdfAsync(orderId);
            return File(pdf, "application/pdf", $"invoice-{orderId}.pdf");
        }
    }
}
