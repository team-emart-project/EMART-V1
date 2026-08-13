using Emart.Api.Middleware;
using Emart.Api.Models;

namespace Emart.Api.Services
{
    /// <summary>The resolved cost of ONE unit under a chosen option.</summary>
    public readonly record struct ResolvedPrice(decimal CashPerUnit, int PointsPerUnit)
    {
        public decimal CashFor(int quantity) => CashPerUnit * quantity;
        public int PointsFor(int quantity) => PointsPerUnit * quantity;
    }

    public interface IPricingService
    {
        ResolvedPrice Resolve(ProductMaster product, PriceOption? option);
        void Validate(ProductMaster product, PriceOption? option, int quantity,
                      bool isCardholder, int pointsAvailable);
    }

    /// <summary>
    /// The single place that turns a <see cref="PriceOption"/> into money and points.
    ///
    /// WHY THIS IS ITS OWN CLASS
    /// Three flows need the same answer: adding to the cart, previewing the
    /// checkout, and placing the order. If each computed the price itself the
    /// preview could quietly disagree with what actually gets charged — the
    /// exact class of bug a shopper notices and never forgives. One method,
    /// called from all three, cannot disagree with itself.
    ///
    /// Nothing here trusts the client. The request carries only WHICH option
    /// was ticked; the numbers are always re-read from the live catalogue row.
    /// </summary>
    public class PricingService : IPricingService
    {
        /// <summary>Can non-cardholders use the member options? Normally no.</summary>
        private readonly bool _requireCardholder;

        public PricingService(IConfiguration configuration)
        {
            _requireCardholder = configuration.GetValue("Emart:Cart:RequireCardholderForPoints", true);
        }

        /// <summary>
        /// Throws rather than silently falling back to the normal price: if the
        /// UI offered an option the catalogue does not have, that is a bug worth
        /// surfacing, not something to paper over by charging a different amount
        /// than the shopper saw.
        /// </summary>
        public ResolvedPrice Resolve(ProductMaster product, PriceOption? option)
        {
            var chosen = option ?? PriceOption.REGULAR;

            switch (chosen)
            {
                case PriceOption.REGULAR:
                    return new ResolvedPrice(product.MrpPrice, 0);

                case PriceOption.MEMBER:
                    RequireOffer(product.HasMemberOffer, product, "member price");
                    return new ResolvedPrice(product.CardholderPrice!.Value, 0);

                // Cash is exactly zero here — the whole point of Option 2 is
                // that the shopper pays nothing but points.
                case PriceOption.POINTS:
                    RequireOffer(product.HasPointsOffer, product, "points-only price");
                    return new ResolvedPrice(decimal.Zero, product.PointsPrice!.Value);

                case PriceOption.HYBRID:
                    RequireOffer(product.HasHybridOffer, product, "combo price");
                    return new ResolvedPrice(product.HybridCashPrice!.Value, product.HybridPoints!.Value);

                default:
                    return new ResolvedPrice(product.MrpPrice, 0);
            }
        }

        /// <summary>
        /// Checks in the order a person would: are you allowed to use this
        /// option at all, does the product offer it, and can you afford the points.
        /// </summary>
        public void Validate(ProductMaster product, PriceOption? option, int quantity,
                             bool isCardholder, int pointsAvailable)
        {
            var chosen = option ?? PriceOption.REGULAR;

            if (chosen.RequiresCardholder() && _requireCardholder && !isCardholder)
            {
                throw new BusinessRuleViolationException(
                    "Only e-MART cardholders can use member pricing. Apply for a card to unlock it.");
            }

            if (quantity < 1)
            {
                throw new BusinessRuleViolationException("Quantity must be at least 1");
            }

            // Resolve() does the "is this offer actually on the product" check
            // and throws with a clear message if not, so we reuse it rather than
            // repeating the three null checks here.
            var price = Resolve(product, chosen);

            if (chosen.SpendsPoints())
            {
                int needed = price.PointsFor(quantity);
                if (needed > pointsAvailable)
                {
                    throw new BusinessRuleViolationException(
                        $"You don't have enough e-Points. This needs {needed}, you have {pointsAvailable}.");
                }
            }
        }

        private static void RequireOffer(bool available, ProductMaster product, string offerName)
        {
            if (!available)
            {
                throw new BusinessRuleViolationException(
                    $"'{product.ProdName}' is not available at the {offerName}.");
            }
        }
    }
}
