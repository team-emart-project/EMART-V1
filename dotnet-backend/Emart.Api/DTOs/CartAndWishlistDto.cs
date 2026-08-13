using System;
using System.Collections.Generic;
using Emart.Api.Models;

namespace Emart.Api.DTOs
{
    public class CartDto
    {
        public int CartId { get; set; }
        public int UserId { get; set; }
        public string Status { get; set; } = string.Empty;
        public bool Cardholder { get; set; }
        public List<CartItemDto> Items { get; set; } = new List<CartItemDto>();
        public int DistinctItemCount { get; set; }
        public int TotalQuantity { get; set; }

        // Every total is computed on READ, never stored, so it cannot drift
        // from the lines it is derived from.
        public decimal SubtotalMrp { get; set; }
        public decimal SubtotalPayable { get; set; }
        public decimal TotalSavings { get; set; }
        public int TotalPointsUsed { get; set; }
    }

    public class CartItemDto
    {
        public int CartItemId { get; set; }
        public int ProdId { get; set; }
        public string ProdName { get; set; } = string.Empty;
        public string? ProdImagePath { get; set; }
        public decimal MrpPrice { get; set; }

        // Echoed back so the cart can render an option switcher. Surfaced only
        // to an actual cardholder, exactly as on the product listing.
        public decimal? CardholderPrice { get; set; }
        public int? PointsPrice { get; set; }
        public decimal? HybridCashPrice { get; set; }
        public int? HybridPoints { get; set; }

        public PriceOption PriceOption { get; set; }
        public decimal UnitPriceApplied { get; set; }
        public int UnitPointsApplied { get; set; }
        public int Quantity { get; set; }
        public decimal LineTotal { get; set; }
        public decimal LineSavings { get; set; }
        public int PointsUsed { get; set; }
    }

    public class CartItemRequest
    {
        public int ProdId { get; set; }
        public int Quantity { get; set; } = 1;

        // Defaults to REGULAR, so a forgotten field never accidentally spends
        // points on the shopper's behalf.
        public PriceOption PriceOption { get; set; } = PriceOption.REGULAR;
    }

    public class UpdateCartItemRequest
    {
        public int Quantity { get; set; }

        // Null means "keep the line's existing choice" — that is what lets the
        // quantity stepper avoid re-stating the pricing decision on every click.
        public PriceOption? PriceOption { get; set; }
    }

    public class WishlistDto
    {
        public int WishlistId { get; set; }
        public int ProdId { get; set; }
        public string ProdName { get; set; } = string.Empty;
        public string? ProdShortDesc { get; set; }
        public string? ProdImagePath { get; set; }
        public decimal MrpPrice { get; set; }
        public decimal? CardholderPrice { get; set; }
        public int? PointsPrice { get; set; }
        public DateTime AddedAt { get; set; }
    }

    public class WishlistRequest
    {
        public int ProdId { get; set; }
    }
}
