using System.Collections.Generic;

namespace Emart.Api.DTOs
{
    /// <summary>
    /// PRICE VISIBILITY RULE
    ///
    /// mrpPrice is always shown, to everyone. All three e-MART card offers are
    /// shown ONLY to a user with an ACTIVE card; for anybody else the mapper
    /// leaves them null, and the global "omit nulls" JSON setting drops them
    /// from the payload entirely — so member pricing cannot be read out of
    /// devtools by someone not entitled to it.
    ///
    /// Null carries a second meaning: "this product does not offer that
    /// option". Both cases hide the same checkbox, so the UI needs one check.
    /// </summary>
    public class ProductDto
    {
        public int ProdId { get; set; }
        public string ProdName { get; set; } = string.Empty;
        public string? ProdShortDesc { get; set; }
        public string? ProdLongDesc { get; set; }

        public decimal MrpPrice { get; set; }
        public decimal? CardholderPrice { get; set; }
        public decimal? CardholderSaving { get; set; }
        public int? PointsPrice { get; set; }
        public decimal? HybridCashPrice { get; set; }
        public int? HybridPoints { get; set; }

        public string? Brand { get; set; }
        public int StockQuantity { get; set; }
        public bool InStock { get; set; }
        public decimal Rating { get; set; }
        public int RatingCount { get; set; }
        public decimal DiscountPercentage { get; set; }
        public string? ProdImagePath { get; set; }
        public int? CatmasterId { get; set; }
        public string? CategoryName { get; set; }

        // List endpoints omit both: a 12-product grid needs one thumbnail each,
        // not sixty image URLs.
        public List<ProductVariantDto>? Variants { get; set; }
        public List<ProductImageDto>? Images { get; set; }
    }

    public class ProductImageDto
    {
        public int ProdImageId { get; set; }
        public string ImageUrl { get; set; } = string.Empty;
        public string? AltText { get; set; }
        public int DisplayOrder { get; set; }
        public bool IsPrimary { get; set; }
    }

    public class ProductVariantDto
    {
        public int ConfigId { get; set; }
        public string ConfigName { get; set; } = string.Empty;
        public List<VariantValueDto> Values { get; set; } = new List<VariantValueDto>();

        public class VariantValueDto
        {
            public int ProdDtlId { get; set; }
            public string Value { get; set; } = string.Empty;
        }
    }
}
