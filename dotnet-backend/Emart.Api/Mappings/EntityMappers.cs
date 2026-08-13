using System;
using System.Collections.Generic;
using System.Linq;
using Emart.Api.DTOs;
using Emart.Api.Models;
using Emart.Api.Services;

namespace Emart.Api.Mappings
{
    /// <summary>
    /// Hand-written mappers rather than AutoMapper conventions.
    ///
    /// Every response in this API applies a RULE that a property-name match
    /// cannot express — hiding member pricing from non-cardholders, masking a
    /// bank account, computing a line total from the live catalogue row. A
    /// convention-based mapper would silently produce a payload that looks
    /// right and leaks or miscalculates, which is exactly the failure mode
    /// worth spending explicit code to avoid.
    /// </summary>
    public static class UserMapper
    {
        public static UserDto ToDto(User user) => new()
        {
            UserId = user.UserId,
            MembershipNo = user.MembershipNo,
            FirstName = user.FirstName,
            LastName = user.LastName,
            Email = user.Email,
            Phone = user.Phone,
            Dob = user.Dob.HasValue ? DateOnly.FromDateTime(user.Dob.Value) : null,
            Gender = user.Gender,
            Education = user.Education,
            Occupation = user.Occupation,
            AnnualIncome = user.AnnualIncome,
            MarketingConsent = user.MarketingConsent,
            Role = user.Role.ToString(),
            Cardholder = user.IsCardholder,
            Active = user.IsActive,
            AuthProvider = user.AuthProvider.ToString(),
            ProfileImageUrl = user.ProfileImageUrl,
            CreatedAt = user.CreatedAt
            // passwordHash is deliberately absent: there is no property to
            // forget to exclude.
        };
    }

    public static class ProductMapper
    {
        /// <summary>
        /// PRICE VISIBILITY RULE — mrpPrice goes to everyone; the three card
        /// offers go only to a user with an ACTIVE card. Stripping them in one
        /// place means adding a fourth offer later cannot leak by being
        /// forgotten in one of two branches.
        /// </summary>
        public static ProductDto ToSummary(ProductMaster product, bool isCardholder)
        {
            var dto = new ProductDto
            {
                ProdId = product.ProdId,
                ProdName = product.ProdName,
                ProdShortDesc = product.ProdShortDesc,
                MrpPrice = product.MrpPrice,
                CardholderPrice = product.CardholderPrice,
                CardholderSaving = Saving(product),
                PointsPrice = product.PointsPrice,
                HybridCashPrice = product.HybridCashPrice,
                HybridPoints = product.HybridPoints,
                Brand = product.Brand,
                StockQuantity = product.StockQuantity,
                InStock = product.StockQuantity > 0,
                Rating = product.Rating,
                RatingCount = product.RatingCount,
                DiscountPercentage = product.DiscountPercentage,
                ProdImagePath = product.ProdImagePath,
                CatmasterId = product.CatmasterId,
                CategoryName = product.Category?.CatName
            };

            if (!isCardholder) HideMemberOffers(dto);
            return dto;
        }

        public static ProductDto ToDetail(ProductMaster product,
                                          List<ProductVariantDto> variants,
                                          List<ProductImage> images,
                                          bool isCardholder)
        {
            var dto = ToSummary(product, isCardholder);
            dto.ProdLongDesc = product.ProdLongDesc;
            dto.Variants = variants;
            dto.Images = images.Select(ToImageDto).ToList();
            return dto;
        }

        public static ProductImageDto ToImageDto(ProductImage image) => new()
        {
            ProdImageId = image.ProdImageId,
            ImageUrl = image.ImageUrl,
            AltText = image.AltText,
            DisplayOrder = image.DisplayOrder,
            IsPrimary = image.IsPrimary
        };

        /// <summary>
        /// Turns flat prod_dtl_master rows into one entry per attribute.
        /// The dictionary preserves insertion order, which is the ordering the
        /// query already applied.
        /// </summary>
        public static List<ProductVariantDto> GroupVariants(IEnumerable<ProdDtlMaster> details)
        {
            var grouped = new Dictionary<int, ProductVariantDto>();
            var order = new List<int>();

            foreach (var detail in details)
            {
                if (detail.Config == null) continue;
                int configId = detail.Config.ConfigId;

                if (!grouped.TryGetValue(configId, out var group))
                {
                    group = new ProductVariantDto
                    {
                        ConfigId = configId,
                        ConfigName = detail.Config.ConfigName
                    };
                    grouped[configId] = group;
                    order.Add(configId);
                }

                group.Values.Add(new ProductVariantDto.VariantValueDto
                {
                    ProdDtlId = detail.ProdDtlId,
                    Value = detail.ConfigDtls
                });
            }

            return order.Select(id => grouped[id]).ToList();
        }

        private static void HideMemberOffers(ProductDto dto)
        {
            dto.CardholderPrice = null;
            dto.CardholderSaving = null;
            dto.PointsPrice = null;
            dto.HybridCashPrice = null;
            dto.HybridPoints = null;
        }

        /// <summary>Null rather than zero when there is no member price, so the
        /// omit-nulls setting drops the field instead of showing "save ₹0".</summary>
        private static decimal? Saving(ProductMaster product) =>
            product.CardholderPrice.HasValue ? product.MrpPrice - product.CardholderPrice.Value : null;
    }

    public static class CategoryMapper
    {
        public static CategoryDto ToDto(CategoryMaster category) => new()
        {
            CatmasterId = category.CatmasterId,
            CatId = category.CatId,
            SubcatId = category.SubcatId,
            CatName = category.CatName,
            CatImagePath = category.CatImagePath,
            Flag = category.Flag
        };
    }

    public static class AddressMapper
    {
        public static AddressDto? ToDto(Address? address)
        {
            if (address == null) return null;
            return new AddressDto
            {
                AddressId = address.AddressId,
                AddressLine1 = address.AddressLine1,
                AddressLine2 = address.AddressLine2,
                City = address.City,
                State = address.State,
                ZipCode = address.ZipCode,
                Country = address.Country,
                AddressType = address.AddressType.ToString(),
                IsDefault = address.IsDefault,
                CreatedAt = address.CreatedAt
            };
        }
    }

    public static class EmartCardMapper
    {
        public static EmartCardDto ToDto(EmartCard card) => new()
        {
            CardId = card.CardId,
            CardNumber = card.CardNumber,
            Status = card.Status.ToString(),
            ApplicationDate = DateOnly.FromDateTime(card.ApplicationDate),
            ApprovalDate = card.ApprovalDate.HasValue ? DateOnly.FromDateTime(card.ApprovalDate.Value) : null,
            PointsBalance = card.PointsBalance,
            EmploymentDetails = card.EmploymentDetails,
            BankAccountMasked = Mask(card.BankAccountNo)
            // panNumber has no property here at all — it is never returned.
        };

        /// <summary>Shows only the last four digits: "********9012".</summary>
        private static string? Mask(string? accountNo)
        {
            if (string.IsNullOrWhiteSpace(accountNo)) return null;
            if (accountNo.Length <= 4) return new string('*', accountNo.Length);
            return new string('*', accountNo.Length - 4) + accountNo[^4..];
        }
    }

    public static class WishlistMapper
    {
        public static WishlistDto ToDto(Wishlist entry, bool isCardholder)
        {
            var product = entry.Product;
            return new WishlistDto
            {
                WishlistId = entry.WishlistId,
                ProdId = entry.ProdId,
                ProdName = product?.ProdName ?? string.Empty,
                ProdShortDesc = product?.ProdShortDesc,
                ProdImagePath = product?.ProdImagePath,
                MrpPrice = product?.MrpPrice ?? 0m,
                CardholderPrice = isCardholder ? product?.CardholderPrice : null,
                PointsPrice = isCardholder ? product?.PointsPrice : null,
                AddedAt = entry.AddedAt
            };
        }
    }

    public static class PaymentMapper
    {
        public static PaymentDto ToDto(Payment payment) => new()
        {
            PaymentId = payment.PaymentId,
            OrderId = payment.OrderId,
            OrderNo = payment.Order?.OrderNo,
            PaymentMethod = payment.PaymentMethod,
            CardLast4 = payment.CardLast4,
            Amount = payment.Amount,
            Status = payment.Status.ToString(),
            TransactionRef = payment.TransactionRef,
            TransactionDate = payment.TransactionDate
        };
    }

    public static class CartMapper
    {
        public static CartItemDto ToItemDto(CartItem item, bool cardholder, IPricingService pricing)
        {
            var product = item.Product!;
            int quantity = item.Quantity;
            var option = item.PriceOption;

            // Re-resolved from the LIVE catalogue row, never from whatever was
            // stored when the item went into the cart: a price can change while
            // a cart sits open, and the shopper must see today's number.
            var price = pricing.Resolve(product, option);
            decimal lineTotal = price.CashFor(quantity);
            decimal lineAtMrp = product.MrpPrice * quantity;

            return new CartItemDto
            {
                CartItemId = item.CartItemId,
                ProdId = product.ProdId,
                ProdName = product.ProdName,
                ProdImagePath = product.ProdImagePath,
                MrpPrice = product.MrpPrice,
                CardholderPrice = cardholder ? product.CardholderPrice : null,
                PointsPrice = cardholder ? product.PointsPrice : null,
                HybridCashPrice = cardholder ? product.HybridCashPrice : null,
                HybridPoints = cardholder ? product.HybridPoints : null,
                PriceOption = option,
                UnitPriceApplied = price.CashPerUnit,
                UnitPointsApplied = price.PointsPerUnit,
                Quantity = quantity,
                LineTotal = lineTotal,
                LineSavings = lineAtMrp - lineTotal,
                PointsUsed = price.PointsFor(quantity)
            };
        }

        public static CartDto ToCartDto(Cart cart, bool cardholder, IPricingService pricing)
        {
            var items = cart.Items
                .Where(i => i.Product != null)
                .Select(i => ToItemDto(i, cardholder, pricing))
                .ToList();

            decimal subtotalPayable = items.Sum(i => i.LineTotal);
            decimal subtotalMrp = items.Sum(i => i.MrpPrice * i.Quantity);

            return new CartDto
            {
                CartId = cart.CartId,
                UserId = cart.UserId,
                Status = cart.Status.ToString(),
                Cardholder = cardholder,
                Items = items,
                DistinctItemCount = items.Count,
                TotalQuantity = items.Sum(i => i.Quantity),
                SubtotalMrp = subtotalMrp,
                SubtotalPayable = subtotalPayable,
                TotalSavings = subtotalMrp - subtotalPayable,
                TotalPointsUsed = items.Sum(i => i.PointsUsed)
            };
        }
    }

    public static class OrderMapper
    {
        public static OrderDetailDto ToItemDto(OrderDetail item)
        {
            decimal lineTotal = item.PriceCharged * item.Quantity;
            decimal lineAtMrp = item.MrpPrice * item.Quantity;

            return new OrderDetailDto
            {
                OrderDtlId = item.OrderDtlId,
                ProdId = item.ProdId,
                ProdName = item.ProdNameSnapshot,
                Quantity = item.Quantity,
                MrpPrice = item.MrpPrice,
                CardholderPrice = item.CardholderPrice,
                PriceOption = item.PriceOption,
                PriceCharged = item.PriceCharged,
                LineTotal = lineTotal,
                LineSavings = lineAtMrp - lineTotal,
                PointsRedeemed = item.PointsRedeemed
            };
        }

        public static OrderDto ToDto(Orders order, int? pointsBalanceAfter)
        {
            var items = order.Items.Select(ToItemDto).ToList();
            decimal subtotalMrp = items.Sum(i => i.MrpPrice * i.Quantity);

            return new OrderDto
            {
                OrderId = order.OrderId == 0 ? null : order.OrderId,
                OrderNo = string.IsNullOrEmpty(order.OrderNo) ? null : order.OrderNo,
                OrderDate = order.OrderDate,
                CustomerName = order.User?.FullName ?? string.Empty,
                MembershipNo = order.User?.MembershipNo,
                Cardholder = order.User?.IsCardholder ?? false,
                ShippingAddress = AddressMapper.ToDto(order.ShippingAddress),
                BillingAddress = AddressMapper.ToDto(order.BillingAddress),
                Items = items,
                SubtotalMrp = subtotalMrp,
                SubtotalAmount = order.SubtotalAmount,
                TotalSavings = subtotalMrp - order.SubtotalAmount,
                TotalAmount = order.TotalAmount,
                PointsRedeemed = order.PointsRedeemed,
                PointsEarned = order.PointsEarned,
                PointsBalanceAfter = pointsBalanceAfter,
                PaymentStatus = order.PaymentStatus.ToString(),
                OrderStatus = order.OrderStatus.ToString()
            };
        }
    }
}
