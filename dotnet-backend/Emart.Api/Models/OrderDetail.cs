using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace Emart.Api.Models
{
    // The MySQL table is `order_details`, plural. It was mapped to
    // `order_detail` here, so every order read failed with "table doesn't exist".
    [Table("order_details")]
    public class OrderDetail
    {
        [Key]
        [Column("order_dtl_id")]
        public int OrderDtlId { get; set; }

        [Column("order_id")]
        public int OrderId { get; set; }

        [ForeignKey("OrderId")]
        public Orders? Order { get; set; }

        [Column("prod_id")]
        public int ProdId { get; set; }

        [ForeignKey("ProdId")]
        public ProductMaster? Product { get; set; }

        [Column("prod_name_snapshot")]
        [MaxLength(255)]
        public string ProdNameSnapshot { get; set; } = string.Empty;

        [Required]
        [Column("quantity")]
        public int Quantity { get; set; }

        [Column("mrp_price")]
        public decimal MrpPrice { get; set; }

        // NULL when the product carried no member offer at order time.
        [Column("cardholder_price")]
        public decimal? CardholderPrice { get; set; }

        [Required]
        [Column("price_option")]
        public PriceOption PriceOption { get; set; }

        [Column("price_charged")]
        public decimal PriceCharged { get; set; }

        [Column("points_redeemed")]
        public int PointsRedeemed { get; set; }
    }
}
