using Emart.Api.Middleware;
using Emart.Api.Models;
using Emart.Api.Services;
using Microsoft.Extensions.Configuration;
using NUnit.Framework;

namespace Emart.Tests.Services
{
    /// <summary>
    /// PricingService is the one class three separate flows depend on agreeing
    /// with itself — add-to-cart, checkout preview and place-order all call it.
    /// If it is wrong, a shopper is charged something other than what they saw.
    /// </summary>
    [TestFixture]
    public class PricingServiceTests
    {
        private PricingService _pricing = null!;

        [SetUp]
        public void Setup()
        {
            var configuration = new ConfigurationBuilder().AddInMemoryCollection().Build();
            _pricing = new PricingService(configuration);
        }

        private static ProductMaster FullOfferProduct() => new()
        {
            ProdId = 16,
            ProdName = "Lava A1 Josh",
            MrpPrice = 1300m,
            CardholderPrice = 1100m,
            PointsPrice = 1100,
            HybridCashPrice = 650m,
            HybridPoints = 450,
            StockQuantity = 5
        };

        private static ProductMaster MrpOnlyProduct() => new()
        {
            ProdId = 1,
            ProdName = "Plain Product",
            MrpPrice = 500m,
            StockQuantity = 5
        };

        [Test]
        public void Resolve_Regular_ChargesMrpAndNoPoints()
        {
            var price = _pricing.Resolve(FullOfferProduct(), PriceOption.REGULAR);

            Assert.That(price.CashPerUnit, Is.EqualTo(1300m));
            Assert.That(price.PointsPerUnit, Is.EqualTo(0));
        }

        [Test]
        public void Resolve_NullOption_FallsBackToRegular()
        {
            var price = _pricing.Resolve(FullOfferProduct(), null);

            Assert.That(price.CashPerUnit, Is.EqualTo(1300m));
        }

        [Test]
        public void Resolve_Points_ChargesZeroCash()
        {
            var price = _pricing.Resolve(FullOfferProduct(), PriceOption.POINTS);

            Assert.That(price.CashPerUnit, Is.EqualTo(0m),
                "the whole point of the points-only option is that no cash is charged");
            Assert.That(price.PointsPerUnit, Is.EqualTo(1100));
        }

        [Test]
        public void Resolve_Hybrid_SplitsCashAndPoints()
        {
            var price = _pricing.Resolve(FullOfferProduct(), PriceOption.HYBRID);

            Assert.That(price.CashPerUnit, Is.EqualTo(650m));
            Assert.That(price.PointsPerUnit, Is.EqualTo(450));
            Assert.That(price.CashFor(2), Is.EqualTo(1300m));
            Assert.That(price.PointsFor(2), Is.EqualTo(900));
        }

        [Test]
        public void Resolve_OfferTheProductDoesNotCarry_Throws()
        {
            // Refusing is deliberate: silently charging MRP would bill a
            // different number than the one the shopper was shown.
            Assert.Throws<BusinessRuleViolationException>(
                () => _pricing.Resolve(MrpOnlyProduct(), PriceOption.MEMBER));
        }

        [Test]
        public void Validate_NonCardholderPickingMemberPrice_Throws()
        {
            Assert.Throws<BusinessRuleViolationException>(
                () => _pricing.Validate(FullOfferProduct(), PriceOption.MEMBER, 1,
                                        isCardholder: false, pointsAvailable: 0));
        }

        [Test]
        public void Validate_NotEnoughPoints_Throws()
        {
            // 450 points/unit x 2 = 900 needed, against a balance of 250.
            var ex = Assert.Throws<BusinessRuleViolationException>(
                () => _pricing.Validate(FullOfferProduct(), PriceOption.HYBRID, 2,
                                        isCardholder: true, pointsAvailable: 250));

            Assert.That(ex!.Message, Does.Contain("900").And.Contain("250"));
        }

        [Test]
        public void Validate_CardholderWithEnoughPoints_Passes()
        {
            Assert.DoesNotThrow(
                () => _pricing.Validate(FullOfferProduct(), PriceOption.HYBRID, 2,
                                        isCardholder: true, pointsAvailable: 900));
        }

        [Test]
        public void Validate_RegularOption_NeedsNoCard()
        {
            Assert.DoesNotThrow(
                () => _pricing.Validate(MrpOnlyProduct(), PriceOption.REGULAR, 3,
                                        isCardholder: false, pointsAvailable: 0));
        }
    }
}
