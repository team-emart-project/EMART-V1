using System;
using System.Collections.Generic;
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
    public interface IPaymentService
    {
        Task<PaymentDto> VerifyPaymentAsync(int orderId, PaymentVerifyRequest request);
        Task<IEnumerable<PaymentDto>> GetPaymentsForOrderAsync(int orderId);
    }

    public class PaymentService : IPaymentService
    {
        /// <summary>Mock gateway: a card number ending in 0 is DECLINED.</summary>
        private const string DeclineSuffix = "0";

        private readonly EmartDbContext _context;
        private readonly ISecurityUtils _securityUtils;
        private readonly ICardholderService _cardholderService;
        private readonly ILogger<PaymentService> _logger;

        public PaymentService(EmartDbContext context,
                              ISecurityUtils securityUtils,
                              ICardholderService cardholderService,
                              ILogger<PaymentService> logger)
        {
            _context = context;
            _securityUtils = securityUtils;
            _cardholderService = cardholderService;
            _logger = logger;
        }

        public async Task<PaymentDto> VerifyPaymentAsync(int orderId, PaymentVerifyRequest request)
        {
            var order = await LoadOwnedOrderAsync(orderId);

            if (order.OrderStatus == OrderStatus.CANCELLED)
            {
                throw new BusinessRuleViolationException("This order has been cancelled");
            }
            if (order.PaymentStatus == PaymentStatus.PAID)
            {
                throw new BusinessRuleViolationException("This order has already been paid");
            }

            // The client sends the amount it thinks it owes. If that disagrees
            // with the server's figure, something was tampered with — refuse.
            if (request.Amount != order.TotalAmount)
            {
                throw new BusinessRuleViolationException(
                    $"Amount {request.Amount} does not match the order total {order.TotalAmount}");
            }

            // An order fully covered by e-Points has nothing to charge, so no
            // card is required and none is asked for. This check lives here
            // rather than as an attribute on the DTO because whether a card is
            // needed depends on the ORDER, which a per-field rule cannot see.
            bool nothingToPay = order.TotalAmount == 0m;
            string cardNumber = request.CardNumber?.Trim() ?? string.Empty;
            string? last4 = null;
            bool approved = true;

            if (!nothingToPay)
            {
                if (string.IsNullOrWhiteSpace(cardNumber))
                {
                    throw new BusinessRuleViolationException("Card details are required to pay this order");
                }
                if (string.IsNullOrWhiteSpace(request.CardHolderName))
                {
                    throw new BusinessRuleViolationException("Card holder name is required");
                }
                if (cardNumber.Length < 4)
                {
                    throw new BusinessRuleViolationException("That card number is not valid");
                }

                last4 = cardNumber[^4..];
                approved = !cardNumber.EndsWith(DeclineSuffix, StringComparison.Ordinal);
            }

            var payment = new Payment
            {
                OrderId = order.OrderId,
                PaymentMethod = nothingToPay ? "POINTS" : "CARD",
                // Only the last 4 digits are stored. Never the full number,
                // never the CVV — neither has a column to be written to.
                CardLast4 = last4,
                Amount = order.TotalAmount,
                Status = approved ? PaymentStatus.SUCCESS : PaymentStatus.FAILED,
                TransactionRef = "TXN-" + Guid.NewGuid().ToString("N")[..12].ToUpperInvariant(),
                TransactionDate = DateTime.Now
            };

            _context.Payments.Add(payment);
            await _context.SaveChangesAsync();

            if (!approved)
            {
                // Leave the order PENDING so the customer can retry with
                // another card. The failed attempt is still recorded for audit.
                _logger.LogInformation("Payment DECLINED for orderNo={OrderNo}", order.OrderNo);
                var declined = PaymentMapper.ToDto(payment);
                declined.OrderNo = order.OrderNo;
                declined.OrderStatus = order.OrderStatus.ToString();
                return declined;
            }

            // ---- success path: order state and points settle together ----
            order.PaymentStatus = PaymentStatus.PAID;
            order.OrderStatus = OrderStatus.PAID;
            order.UpdatedAt = DateTime.Now;
            await _context.SaveChangesAsync();

            int? balanceAfter = await SettlePointsAsync(order);

            _logger.LogInformation("Payment SUCCESS for orderNo={OrderNo} amount={Amount}",
                order.OrderNo, order.TotalAmount);

            var response = PaymentMapper.ToDto(payment);
            response.OrderNo = order.OrderNo;
            response.PointsEarned = order.PointsEarned;
            response.PointsRedeemed = order.PointsRedeemed;
            response.PointsBalanceAfter = balanceAfter;
            response.OrderStatus = order.OrderStatus.ToString();
            return response;
        }

        /// <summary>Every attempt, newest first. Failures are kept for audit.</summary>
        public async Task<IEnumerable<PaymentDto>> GetPaymentsForOrderAsync(int orderId)
        {
            var order = await LoadOwnedOrderAsync(orderId);   // ownership check first

            var payments = await _context.Payments
                .AsNoTracking()
                .Where(p => p.OrderId == orderId)
                .OrderByDescending(p => p.TransactionDate)
                .ThenByDescending(p => p.PaymentId)
                .ToListAsync();

            return payments.Select(p =>
            {
                var dto = PaymentMapper.ToDto(p);
                dto.OrderNo = order.OrderNo;
                return dto;
            }).ToList();
        }

        // ------------------------------------------------------------------

        private async Task<int?> SettlePointsAsync(Orders order)
        {
            var card = await _cardholderService.FindCardAsync(order.UserId);

            if (card == null || card.Status != CardStatus.APPROVED)
            {
                // Not an active cardholder: nothing earned, nothing to debit.
                _logger.LogDebug("userId={UserId} has no APPROVED card - no points settled", order.UserId);
                return null;
            }

            // Re-checked at payment time: the balance may have changed since
            // checkout. This is the third of three checks, and the only one
            // that runs at the moment money actually moves.
            if (order.PointsRedeemed > card.PointsBalance)
            {
                throw new BusinessRuleViolationException(
                    $"Your e-Points balance ({card.PointsBalance}) is no longer enough to redeem {order.PointsRedeemed} points");
            }

            return await _cardholderService.AdjustPointsAsync(card, order.PointsRedeemed, order.PointsEarned);
        }

        private async Task<Orders> LoadOwnedOrderAsync(int orderId)
        {
            var order = await _context.Orders.FirstOrDefaultAsync(o => o.OrderId == orderId)
                        ?? throw new ResourceNotFoundException("Order", "orderId", orderId);

            if (order.UserId != _securityUtils.GetCurrentUserId())
            {
                throw new UnauthorizedActionException("That order does not belong to you");
            }
            return order;
        }
    }
}
