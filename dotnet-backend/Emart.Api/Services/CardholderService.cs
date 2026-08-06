using System;
using System.Threading.Tasks;
using Emart.Api.Data;
using Emart.Api.Models;
using Microsoft.EntityFrameworkCore;

namespace Emart.Api.Services
{
    public interface ICardholderService
    {
        Task<bool> IsActiveCardholderAsync(int? userId);
        Task<bool> IsCurrentUserCardholderAsync();
        Task<int> GetPointsBalanceAsync(int? userId);
        Task<EmartCard?> FindCardAsync(int userId);
        Task<EmartCard> ApproveAsync(EmartCard card, DateTime approvalDate);
        Task<int> AdjustPointsAsync(EmartCard card, int redeemed, int earned);
    }

    /// <summary>
    /// THE single source of truth for "does this user hold an active e-MART card?"
    ///
    /// The schema stores that fact in TWO places:
    ///
    ///     users.is_cardholder     (drives which price the cart charges)
    ///     emart_card.status       (drives whether e-Points are settled)
    ///
    /// Routing every read AND write through this class means the two columns
    /// can no longer disagree — which is what caused e-Points to silently never
    /// update for anyone who applied through the app.
    /// </summary>
    public class CardholderService : ICardholderService
    {
        private readonly EmartDbContext _context;
        private readonly ISecurityUtils _securityUtils;
        private readonly ILogger<CardholderService> _logger;

        public CardholderService(EmartDbContext context,
                                 ISecurityUtils securityUtils,
                                 ILogger<CardholderService> logger)
        {
            _context = context;
            _securityUtils = securityUtils;
            _logger = logger;
        }

        /// <summary>An "active" card is an APPROVED one. PENDING and REJECTED do not count.</summary>
        public async Task<bool> IsActiveCardholderAsync(int? userId)
        {
            if (userId == null) return false;
            return await _context.EmartCards
                .AnyAsync(c => c.UserId == userId && c.Status == CardStatus.APPROVED);
        }

        /// <summary>For PUBLIC endpoints: false for a signed-out visitor, no exception.</summary>
        public Task<bool> IsCurrentUserCardholderAsync() =>
            IsActiveCardholderAsync(_securityUtils.GetCurrentUserIdOrNull());

        /// <summary>Redeemable e-Points. 0 for non-cardholders — never throws.</summary>
        public async Task<int> GetPointsBalanceAsync(int? userId)
        {
            if (userId == null) return 0;
            var card = await _context.EmartCards
                .AsNoTracking()
                .FirstOrDefaultAsync(c => c.UserId == userId && c.Status == CardStatus.APPROVED);
            return card?.PointsBalance ?? 0;
        }

        public async Task<EmartCard?> FindCardAsync(int userId) =>
            await _context.EmartCards.FirstOrDefaultAsync(c => c.UserId == userId);

        /// <summary>
        /// Approves a card and flips users.is_cardholder in the SAME save.
        /// This is the only method allowed to set either value, which is what
        /// stops the two columns drifting apart again.
        /// </summary>
        public async Task<EmartCard> ApproveAsync(EmartCard card, DateTime approvalDate)
        {
            card.Status = CardStatus.APPROVED;
            card.ApprovalDate = approvalDate.Date;

            var user = await _context.Users.FirstOrDefaultAsync(u => u.UserId == card.UserId);
            if (user != null) user.IsCardholder = true;

            await _context.SaveChangesAsync();

            _logger.LogInformation("Card {CardNumber} APPROVED for userId={UserId}; users.is_cardholder set to true",
                card.CardNumber, card.UserId);
            return card;
        }

        /// <summary>Applies a points delta and persists it, so "who is allowed
        /// to change a balance" has exactly one answer.</summary>
        public async Task<int> AdjustPointsAsync(EmartCard card, int redeemed, int earned)
        {
            int newBalance = card.PointsBalance - redeemed + earned;
            card.PointsBalance = newBalance;
            await _context.SaveChangesAsync();

            _logger.LogInformation("Points settled for cardId={CardId}: -{Redeemed} +{Earned} => {Balance}",
                card.CardId, redeemed, earned, newBalance);
            return newBalance;
        }
    }
}
