using System;
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
    public interface IEmartCardService
    {
        Task<EmartCardDto> ApplyAsync(EmartCardApplicationRequest request);
        Task<PointsBalanceResponse> GetMyPointsBalanceAsync();
        Task<EmartCardDto> GetMyCardAsync();
    }

    public class EmartCardService : IEmartCardService
    {
        private readonly EmartDbContext _context;
        private readonly ISecurityUtils _securityUtils;
        private readonly ICardholderService _cardholderService;
        private readonly bool _autoApprove;

        public EmartCardService(EmartDbContext context,
                                ISecurityUtils securityUtils,
                                ICardholderService cardholderService,
                                IConfiguration configuration)
        {
            _context = context;
            _securityUtils = securityUtils;
            _cardholderService = cardholderService;
            _autoApprove = configuration.GetValue("Emart:Card:AutoApprove", true);
        }

        public async Task<EmartCardDto> ApplyAsync(EmartCardApplicationRequest request)
        {
            int userId = _securityUtils.GetCurrentUserId();

            if (await _context.EmartCards.AnyAsync(c => c.UserId == userId))
            {
                throw new DuplicateResourceException(
                    "You already have an e-MART card or a pending application.");
            }

            var card = new EmartCard
            {
                UserId = userId,
                CardNumber = GenerateCardNumber(),
                ApplicationDate = DateTime.Today,
                Status = CardStatus.PENDING,
                PointsBalance = 0,
                EmploymentDetails = request.EmploymentDetails,
                BankAccountNo = request.BankAccountNo,
                PanNumber = request.PanNumber?.ToUpperInvariant(),
                CreatedAt = DateTime.Now
            };

            _context.EmartCards.Add(card);
            await _context.SaveChangesAsync();

            // With auto-approve off a card sits at PENDING forever, because
            // there is no admin approval module in this phase — and every
            // downstream e-Points check requires an APPROVED card.
            if (_autoApprove)
            {
                await _cardholderService.ApproveAsync(card, DateTime.Today);
            }

            return EmartCardMapper.ToDto(card);
        }

        /// <summary>
        /// Backs the "Redeem e-Points" checkbox, so it NEVER 404s: a
        /// non-cardholder gets { cardholder: false, pointsBalance: 0 }.
        /// </summary>
        public async Task<PointsBalanceResponse> GetMyPointsBalanceAsync()
        {
            int? userId = _securityUtils.GetCurrentUserIdOrNull();
            if (userId == null)
            {
                return new PointsBalanceResponse { Cardholder = false, PointsBalance = 0, CardStatus = "NONE" };
            }

            var card = await _context.EmartCards
                .AsNoTracking()
                .FirstOrDefaultAsync(c => c.UserId == userId);

            if (card == null)
            {
                return new PointsBalanceResponse { Cardholder = false, PointsBalance = 0, CardStatus = "NONE" };
            }

            bool active = card.Status == CardStatus.APPROVED;
            return new PointsBalanceResponse
            {
                Cardholder = active,
                PointsBalance = active ? card.PointsBalance : 0,
                CardStatus = card.Status.ToString()
            };
        }

        public async Task<EmartCardDto> GetMyCardAsync()
        {
            int userId = _securityUtils.GetCurrentUserId();

            var card = await _context.EmartCards
                .AsNoTracking()
                .FirstOrDefaultAsync(c => c.UserId == userId)
                ?? throw new ResourceNotFoundException("You do not have an e-MART card yet");

            return EmartCardMapper.ToDto(card);
        }

        private static string GenerateCardNumber() =>
            "EMCARD-" + Random.Shared.Next(1000000, 9999999);
    }
}
