using System;
using System.Collections.Generic;
using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace Emart.Api.Models
{
    [Table("product_master")]
    public class ProductMaster
    {
        [Key]
        [Column("prod_id")]
        public int ProdId { get; set; }

        [Column("catmaster_id")]
        public int CatmasterId { get; set; }

        [ForeignKey("CatmasterId")]
        public CategoryMaster? Category { get; set; }

        [Required]
        [Column("prod_name")]
        [MaxLength(255)]
        public string ProdName { get; set; } = string.Empty;

        [Column("prod_short_desc")]
        [MaxLength(500)]
        public string? ProdShortDesc { get; set; }

        [Column("prod_long_desc")]
        public string? ProdLongDesc { get; set; }

        [Column("mrp_price")]
        public decimal MrpPrice { get; set; }

        // The three e-MART card offers. NULL in MySQL means "this product does
        // not carry that offer", which is why every one of them is nullable —
        // reading a NULL into a plain `decimal` throws at materialisation time.
        [Column("cardholder_price")]
        public decimal? CardholderPrice { get; set; }

        [Column("points_price")]
        public int? PointsPrice { get; set; }

        [Column("hybrid_cash_price")]
        public decimal? HybridCashPrice { get; set; }

        [Column("hybrid_points")]
        public int? HybridPoints { get; set; }

        [Column("brand")]
        [MaxLength(100)]
        public string? Brand { get; set; }

        [Column("stock_quantity")]
        public int StockQuantity { get; set; }

        [Column("rating")]
        public decimal Rating { get; set; }

        [Column("rating_count")]
        public int RatingCount { get; set; }

        [Column("discount_percentage")]
        public decimal DiscountPercentage { get; set; }

        [Column("prod_image_path")]
        [MaxLength(255)]
        public string? ProdImagePath { get; set; }

        [Column("created_at")]
        public DateTime CreatedAt { get; set; } = DateTime.Now;

        public List<ProductImage> Images { get; set; } = new List<ProductImage>();
        public List<ProdDtlMaster> Details { get; set; } = new List<ProdDtlMaster>();

        [NotMapped]
        public bool InStock => StockQuantity > 0;

        [NotMapped]
        public bool HasMemberOffer => CardholderPrice.HasValue;

        [NotMapped]
        public bool HasPointsOffer => PointsPrice.HasValue;

        [NotMapped]
        public bool HasHybridOffer => HybridCashPrice.HasValue && HybridPoints.HasValue;
    }
}
