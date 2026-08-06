using System;
using System.Collections.Generic;
using System.ComponentModel.DataAnnotations;
using Emart.Api.Models;

namespace Emart.Api.DTOs
{
    public class CheckoutRequest
    {
        [Required(ErrorMessage = "A shipping address is required")]
        public int ShippingAddressId { get; set; }

        /// <summary>Optional — defaults to the shipping address.</summary>
        public int? BillingAddressId { get; set; }
    }

    public class OrderDto
    {
        public int? OrderId { get; set; }
        public string? OrderNo { get; set; }
        public DateTime OrderDate { get; set; }
        public string CustomerName { get; set; } = string.Empty;
        public string? MembershipNo { get; set; }
        public bool Cardholder { get; set; }

        public AddressDto? ShippingAddress { get; set; }
        public AddressDto? BillingAddress { get; set; }
        public List<OrderDetailDto> Items { get; set; } = new List<OrderDetailDto>();

        public decimal SubtotalMrp { get; set; }
        public decimal SubtotalAmount { get; set; }
        public decimal TotalSavings { get; set; }

        // No tax anywhere in this project: totalAmount always equals
        // subtotalAmount. Points are not a discount subtracted at the end —
        // a line bought with points simply has a lower cash price already.
        public decimal TotalAmount { get; set; }

        public int PointsRedeemed { get; set; }
        public int PointsEarned { get; set; }
        public int? PointsBalanceAfter { get; set; }

        public string PaymentStatus { get; set; } = string.Empty;
        public string OrderStatus { get; set; } = string.Empty;

        /// <summary>True on a checkout preview, where nothing was saved.</summary>
        public bool? Preview { get; set; }
    }

    public class OrderDetailDto
    {
        public int OrderDtlId { get; set; }
        public int ProdId { get; set; }

        /// <summary>The snapshot taken at order time, so an old invoice never
        /// changes if the catalogue is edited later.</summary>
        public string ProdName { get; set; } = string.Empty;

        public int Quantity { get; set; }
        public decimal MrpPrice { get; set; }
        public decimal? CardholderPrice { get; set; }
        public PriceOption PriceOption { get; set; }
        public decimal PriceCharged { get; set; }
        public decimal LineTotal { get; set; }
        public decimal LineSavings { get; set; }
        public int PointsRedeemed { get; set; }
    }

    public class AddressRequest
    {
        [Required(ErrorMessage = "Address line 1 is required")]
        public string AddressLine1 { get; set; } = string.Empty;

        public string? AddressLine2 { get; set; }

        [Required(ErrorMessage = "City is required")]
        public string City { get; set; } = string.Empty;

        [Required(ErrorMessage = "State is required")]
        public string State { get; set; } = string.Empty;

        [Required(ErrorMessage = "Zip code is required")]
        public string ZipCode { get; set; } = string.Empty;

        public string Country { get; set; } = "India";
        public AddressType AddressType { get; set; } = AddressType.SHIPPING;
        public bool IsDefault { get; set; }
    }

    public class AddressDto
    {
        public int AddressId { get; set; }
        public string AddressLine1 { get; set; } = string.Empty;
        public string? AddressLine2 { get; set; }
        public string City { get; set; } = string.Empty;
        public string State { get; set; } = string.Empty;
        public string ZipCode { get; set; } = string.Empty;
        public string Country { get; set; } = string.Empty;
        public string AddressType { get; set; } = string.Empty;
        public bool IsDefault { get; set; }
        public DateTime CreatedAt { get; set; }
    }

    public class PaymentVerifyRequest
    {
        public string? CardNumber { get; set; }
        public string? CardHolderName { get; set; }
        public string? Expiry { get; set; }
        public string? Cvv { get; set; }

        /// <summary>Must equal the order's totalAmount exactly. That check is
        /// the tamper guard, so it cannot be auto-filled server-side.</summary>
        [Required(ErrorMessage = "Amount is required")]
        public decimal Amount { get; set; }
    }

    public class PaymentDto
    {
        public int PaymentId { get; set; }
        public int OrderId { get; set; }
        public string? OrderNo { get; set; }
        public string PaymentMethod { get; set; } = string.Empty;
        public string? CardLast4 { get; set; }
        public decimal Amount { get; set; }
        public string Status { get; set; } = string.Empty;
        public string? TransactionRef { get; set; }
        public DateTime TransactionDate { get; set; }
        public int? PointsEarned { get; set; }
        public int? PointsRedeemed { get; set; }
        public int? PointsBalanceAfter { get; set; }
        public string? OrderStatus { get; set; }
    }
}
