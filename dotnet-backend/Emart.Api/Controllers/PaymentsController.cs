using System.Collections.Generic;
using System.Threading.Tasks;
using Emart.Api.DTOs;
using Emart.Api.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace Emart.Api.Controllers
{
    [ApiController]
    [Route("api/payments")]
    [Authorize]
    public class PaymentsController : ControllerBase
    {
        private readonly IPaymentService _paymentService;

        public PaymentsController(IPaymentService paymentService)
        {
            _paymentService = paymentService;
        }

        [HttpPost("{orderId}/verify")]
        public async Task<ActionResult<ApiResponse<PaymentDto>>> VerifyPayment(int orderId, [FromBody] PaymentVerifyRequest request)
        {
            var response = await _paymentService.VerifyPaymentAsync(orderId, request);
            
            string message = response.Status == nameof(Models.PaymentStatus.SUCCESS)
                ? "Payment successful. Your invoice is ready to download."
                : "Payment was declined. Please try a different card.";

            return Ok(ApiResponse<PaymentDto>.CreateSuccess(message, response));
        }

        [HttpGet("{orderId}")]
        public async Task<ActionResult<ApiResponse<IEnumerable<PaymentDto>>>> GetPayments(int orderId)
        {
            var payments = await _paymentService.GetPaymentsForOrderAsync(orderId);
            return Ok(ApiResponse<IEnumerable<PaymentDto>>.CreateSuccess("Payments retrieved successfully", payments));
        }
    }
}
