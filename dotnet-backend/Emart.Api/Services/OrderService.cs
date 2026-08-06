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
    public interface IOrderService
    {
        Task<OrderDto> CheckoutPreviewAsync(CheckoutRequest request);
        Task<OrderDto> PlaceOrderAsync(CheckoutRequest request);
        Task<PageResponse<OrderDto>> GetMyOrdersAsync(int page, int size);
        Task<OrderDto> GetOrderAsync(int orderId);
        Task<OrderDto> CancelOrderAsync(int orderId);
        Task<byte[]> GenerateInvoicePdfAsync(int orderId);
    }

    public class OrderService : IOrderService
    {
        private readonly EmartDbContext _context;
        private readonly ISecurityUtils _securityUtils;
        private readonly ICardholderService _cardholderService;
        private readonly IPricingService _pricingService;
        private readonly IConfiguration _configuration;
        private readonly ILogger<OrderService> _logger;

        public OrderService(EmartDbContext context,
                            ISecurityUtils securityUtils,
                            ICardholderService cardholderService,
                            IPricingService pricingService,
                            IConfiguration configuration,
                            ILogger<OrderService> logger)
        {
            _context = context;
            _securityUtils = securityUtils;
            _cardholderService = cardholderService;
            _pricingService = pricingService;
            _configuration = configuration;
            _logger = logger;
        }

        // ------------------------------------------------------------------
        // Preview — computes every total but saves NOTHING
        // ------------------------------------------------------------------
        public async Task<OrderDto> CheckoutPreviewAsync(CheckoutRequest request)
        {
            var user = await LoadCurrentUserAsync();
            var cart = await LoadCartWithItemsAsync(user);
            var (shipping, billing) = await LoadAddressesAsync(request, user);

            var draft = await BuildOrderAsync(user, cart, shipping, billing);

            var dto = OrderMapper.ToDto(draft, null);
            dto.Preview = true;
            // Nothing was persisted, so these two are meaningless on a preview.
            dto.OrderId = null;
            dto.OrderNo = null;
            return dto;
        }

        // ------------------------------------------------------------------
        // Place
        // ------------------------------------------------------------------
        public async Task<OrderDto> PlaceOrderAsync(CheckoutRequest request)
        {
            var user = await LoadCurrentUserAsync();
            var cart = await LoadCartWithItemsAsync(user);
            var (shipping, billing) = await LoadAddressesAsync(request, user);

            await using var transaction = await _context.Database.BeginTransactionAsync();

            var order = await BuildOrderAsync(user, cart, shipping, billing);
            order.OrderNo = await GenerateOrderNumberAsync();
            order.OrderDate = DateTime.Now;
            order.CreatedAt = DateTime.Now;

            // Only the ids are written; the navigation objects came back from a
            // no-tracking query and attaching them would re-insert rows.
            order.User = null;
            order.ShippingAddress = null;
            order.BillingAddress = null;

            _context.Orders.Add(order);
            await _context.SaveChangesAsync();

            // The cart ROW is reused, not replaced: cart.user_id is UNIQUE, so a
            // user cannot hold a CONVERTED cart and a fresh ACTIVE one at once.
            // Emptying it leaves the same row ready for the next shop.
            var items = await _context.CartItems.Where(i => i.CartId == cart.CartId).ToListAsync();
            _context.CartItems.RemoveRange(items);

            var cartRow = await _context.Carts.FirstAsync(c => c.CartId == cart.CartId);
            cartRow.Status = CartStatus.ACTIVE;
            cartRow.UpdatedAt = DateTime.Now;

            await _context.SaveChangesAsync();
            await transaction.CommitAsync();

            _logger.LogInformation("Placed orderNo={OrderNo} for userId={UserId} total={Total}",
                order.OrderNo, user.UserId, order.TotalAmount);

            order.User = user;
            order.ShippingAddress = shipping;
            order.BillingAddress = billing;
            return OrderMapper.ToDto(order, null);
        }

        // ------------------------------------------------------------------
        // Read
        // ------------------------------------------------------------------
        public async Task<PageResponse<OrderDto>> GetMyOrdersAsync(int page, int size)
        {
            int userId = _securityUtils.GetCurrentUserId();
            page = Math.Max(0, page);
            size = Math.Clamp(size, 1, 100);

            var query = _context.Orders.AsNoTracking().Where(o => o.UserId == userId);

            int totalElements = await query.CountAsync();
            var orders = await query
                .Include(o => o.User)
                .Include(o => o.ShippingAddress)
                .Include(o => o.BillingAddress)
                .Include(o => o.Items)
                .OrderByDescending(o => o.OrderDate)
                .ThenByDescending(o => o.OrderId)
                .Skip(page * size)
                .Take(size)
                .ToListAsync();

            var content = orders.Select(o => OrderMapper.ToDto(o, null)).ToList();
            int totalPages = (int)Math.Ceiling(totalElements / (double)size);

            return new PageResponse<OrderDto>(
                content, page, size, totalElements, totalPages,
                page == 0, page >= totalPages - 1);
        }

        public async Task<OrderDto> GetOrderAsync(int orderId)
        {
            var order = await LoadOwnedOrderAsync(orderId, track: false);
            return OrderMapper.ToDto(order, null);
        }

        // ------------------------------------------------------------------
        // Cancel
        // ------------------------------------------------------------------
        public async Task<OrderDto> CancelOrderAsync(int orderId)
        {
            var order = await LoadOwnedOrderAsync(orderId, track: true);

            // The state machine is enforced on the SERVER. Never trust the
            // client to know whether cancelling is allowed.
            if (order.OrderStatus == OrderStatus.CANCELLED)
            {
                throw new BusinessRuleViolationException("This order is already cancelled");
            }
            if (order.PaymentStatus == PaymentStatus.PAID)
            {
                throw new BusinessRuleViolationException(
                    "A paid order cannot be cancelled here. Please raise a refund request.");
            }
            if (order.OrderStatus != OrderStatus.PLACED)
            {
                throw new BusinessRuleViolationException(
                    "Only an order still in PLACED status can be cancelled");
            }

            order.OrderStatus = OrderStatus.CANCELLED;
            order.UpdatedAt = DateTime.Now;
            await _context.SaveChangesAsync();

            _logger.LogInformation("Cancelled orderNo={OrderNo}", order.OrderNo);
            return OrderMapper.ToDto(order, null);
        }

        // ------------------------------------------------------------------
        // Invoice
        // ------------------------------------------------------------------
        public async Task<byte[]> GenerateInvoicePdfAsync(int orderId)
        {
            var order = await LoadOwnedOrderAsync(orderId, track: false);

            if (order.PaymentStatus != PaymentStatus.PAID)
            {
                throw new BusinessRuleViolationException(
                    "An invoice is only available once the order has been paid");
            }

            return InvoicePdfGenerator.Generate(order);
        }

        // ==================================================================
        // Calculation
        // ==================================================================

        /// <summary>
        /// Builds the order graph in memory. Called by BOTH preview and place,
        /// which is what guarantees the number a shopper is shown is the number
        /// they are charged.
        /// </summary>
        private async Task<Orders> BuildOrderAsync(User user, Cart cart, Address shipping, Address billing)
        {
            // Derived from emart_card.status, not the denormalised
            // users.is_cardholder flag — the two used to drift apart.
            bool cardholder = await _cardholderService.IsActiveCardholderAsync(user.UserId);
            int pointsAvailable = await _cardholderService.GetPointsBalanceAsync(user.UserId);

            var order = new Orders
            {
                UserId = user.UserId,
                User = user,
                ShippingAddressId = shipping.AddressId,
                ShippingAddress = shipping,
                BillingAddressId = billing.AddressId,
                BillingAddress = billing,
                OrderDate = DateTime.Now,
                PaymentStatus = PaymentStatus.PENDING,
                OrderStatus = OrderStatus.PLACED,
                Items = new List<OrderDetail>()
            };

            decimal subtotal = decimal.Zero;
            int totalPointsRedeemed = 0;

            foreach (var cartItem in cart.Items)
            {
                var product = cartItem.Product!;
                int quantity = cartItem.Quantity;
                var option = cartItem.PriceOption;

                // Re-validated and re-priced from the LIVE product row, not from
                // whatever was stored when the item went into the cart. A price
                // or an offer can change while a cart sits open, and the shopper
                // must be charged today's rules, not last week's.
                _pricingService.Validate(product, option, quantity, cardholder, pointsAvailable);
                var price = _pricingService.Resolve(product, option);

                subtotal += price.CashFor(quantity);
                totalPointsRedeemed += price.PointsFor(quantity);

                order.Items.Add(new OrderDetail
                {
                    ProdId = product.ProdId,
                    // Snapshot the NAME so an old invoice never changes if the
                    // catalogue is edited later.
                    ProdNameSnapshot = product.ProdName,
                    Quantity = quantity,
                    MrpPrice = product.MrpPrice,
                    CardholderPrice = product.CardholderPrice,
                    PriceOption = option,
                    PriceCharged = price.CashPerUnit,
                    PointsRedeemed = price.PointsFor(quantity)
                });
            }

            subtotal = Math.Round(subtotal, 2, MidpointRounding.AwayFromZero);

            // Whole-basket points check. Each line was validated on its own
            // above, but three affordable lines can still add up to more points
            // than the shopper has.
            if (totalPointsRedeemed > pointsAvailable)
            {
                throw new BusinessRuleViolationException(
                    $"This order needs {totalPointsRedeemed} e-Points but you only have {pointsAvailable}");
            }

            // NO TAX and no points "discount": the option a shopper picked
            // already decided the cash price for that line, so the total is
            // simply the sum.
            decimal total = subtotal;

            order.SubtotalAmount = subtotal;
            order.TotalAmount = total;
            order.PointsRedeemed = totalPointsRedeemed;
            // Only cardholders earn points, and only on the cash actually spent.
            order.PointsEarned = cardholder ? CalculatePointsEarned(total) : 0;

            return order;
        }

        /// <summary>
        /// Tiered 2–5% of order value, rounded DOWN.
        ///   under 5,000 -> 2% | 5,000–24,999 -> 3% | 25,000–74,999 -> 4% | 75,000+ -> 5%
        /// </summary>
        internal int CalculatePointsEarned(decimal orderTotal)
        {
            if (orderTotal <= 0) return 0;

            decimal tier1Limit = _configuration.GetValue("Emart:Points:Earn:Tier1Limit", 5000m);
            decimal tier2Limit = _configuration.GetValue("Emart:Points:Earn:Tier2Limit", 25000m);
            decimal tier3Limit = _configuration.GetValue("Emart:Points:Earn:Tier3Limit", 75000m);

            decimal rate =
                orderTotal < tier1Limit ? _configuration.GetValue("Emart:Points:Earn:Tier1Percentage", 2m)
              : orderTotal < tier2Limit ? _configuration.GetValue("Emart:Points:Earn:Tier2Percentage", 3m)
              : orderTotal < tier3Limit ? _configuration.GetValue("Emart:Points:Earn:Tier3Percentage", 4m)
              : _configuration.GetValue("Emart:Points:Earn:Tier4Percentage", 5m);

            return (int)Math.Floor(orderTotal * rate / 100m);
        }

        // ==================================================================
        // Loading + ownership
        // ==================================================================

        private async Task<User> LoadCurrentUserAsync()
        {
            int userId = _securityUtils.GetCurrentUserId();
            return await _context.Users.AsNoTracking().FirstOrDefaultAsync(u => u.UserId == userId)
                   ?? throw new ResourceNotFoundException("User", "userId", userId);
        }

        private async Task<Cart> LoadCartWithItemsAsync(User user)
        {
            var cart = await _context.Carts
                .AsNoTracking()
                .Include(c => c.Items)
                    .ThenInclude(i => i.Product)
                .FirstOrDefaultAsync(c => c.UserId == user.UserId);

            if (cart == null || cart.Items.Count == 0)
            {
                throw new BusinessRuleViolationException(
                    "Your cart is empty. Add something before checking out.");
            }
            return cart;
        }

        private async Task<(Address Shipping, Address Billing)> LoadAddressesAsync(
            CheckoutRequest request, User user)
        {
            var shipping = await LoadOwnedAddressAsync(request.ShippingAddressId, user);

            // billingAddressId is optional and defaults to the shipping address.
            var billing = request.BillingAddressId == null || request.BillingAddressId == shipping.AddressId
                ? shipping
                : await LoadOwnedAddressAsync(request.BillingAddressId.Value, user);

            return (shipping, billing);
        }

        private async Task<Address> LoadOwnedAddressAsync(int addressId, User user)
        {
            var address = await _context.Addresses.AsNoTracking()
                              .FirstOrDefaultAsync(a => a.AddressId == addressId)
                          ?? throw new ResourceNotFoundException("Address", "addressId", addressId);

            if (address.UserId != user.UserId)
            {
                throw new UnauthorizedActionException("That address does not belong to you");
            }
            return address;
        }

        private async Task<Orders> LoadOwnedOrderAsync(int orderId, bool track)
        {
            IQueryable<Orders> query = _context.Orders
                .Include(o => o.User)
                .Include(o => o.ShippingAddress)
                .Include(o => o.BillingAddress)
                .Include(o => o.Items);

            if (!track) query = query.AsNoTracking();

            var order = await query.FirstOrDefaultAsync(o => o.OrderId == orderId)
                        ?? throw new ResourceNotFoundException("Order", "orderId", orderId);

            if (order.UserId != _securityUtils.GetCurrentUserId())
            {
                throw new UnauthorizedActionException("That order does not belong to you");
            }
            return order;
        }

        /// <summary>ORD-2026-048372 style, checked for collisions.</summary>
        private async Task<string> GenerateOrderNumberAsync()
        {
            for (int attempt = 0; attempt < 10; attempt++)
            {
                string candidate = $"ORD-{DateTime.Now:yyyy}-{Random.Shared.Next(100000, 999999)}";
                if (!await _context.Orders.AnyAsync(o => o.OrderNo == candidate))
                {
                    return candidate;
                }
            }
            return $"ORD-{DateTime.Now:yyyy}-{DateTime.Now.Ticks.ToString()[^8..]}";
        }
    }
}
