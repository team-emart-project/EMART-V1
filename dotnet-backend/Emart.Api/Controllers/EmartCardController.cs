using System.Threading.Tasks;
using Emart.Api.DTOs;
using Emart.Api.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace Emart.Api.Controllers
{
    [ApiController]
    [Route("api/emart-card")]
    [Authorize]
    public class EmartCardController : ControllerBase
    {
        private readonly IEmartCardService _emartCardService;

        public EmartCardController(IEmartCardService emartCardService)
        {
            _emartCardService = emartCardService;
        }

        [HttpPost("apply")]
        public async Task<ActionResult<ApiResponse<EmartCardDto>>> Apply([FromBody] EmartCardApplicationRequest request)
        {
            var card = await _emartCardService.ApplyAsync(request);

            // The message follows the card's ACTUAL status. Applications
            // auto-approve by default, and saying "pending review" for a card
            // that is already APPROVED sends the user looking for an approval
            // step that does not exist.
            string message = card.Status == nameof(Models.CardStatus.APPROVED)
                ? "e-MART card approved. Member pricing and e-Points are now unlocked."
                : "e-MART card application submitted. It is now pending review.";

            return StatusCode(201, ApiResponse<EmartCardDto>.CreateSuccess(message, card));
        }

        [HttpGet("balance")]
        [AllowAnonymous]
        public async Task<ActionResult<ApiResponse<PointsBalanceResponse>>> GetMyPointsBalance()
        {
            var balance = await _emartCardService.GetMyPointsBalanceAsync();
            return Ok(ApiResponse<PointsBalanceResponse>.CreateSuccess("Points balance retrieved successfully", balance));
        }

        [HttpGet("me")]
        public async Task<ActionResult<ApiResponse<EmartCardDto>>> GetMyCard()
        {
            var card = await _emartCardService.GetMyCardAsync();
            return Ok(ApiResponse<EmartCardDto>.CreateSuccess("Card retrieved successfully", card));
        }
    }
}
