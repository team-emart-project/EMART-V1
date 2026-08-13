using System;
using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace Emart.Api.Models
{
    [Table("cart_items")]
    public class CartItem
    {
        [Key]
        [Column("cart_item_id")]
        public int CartItemId { get; set; }

        [Column("cart_id")]
        public int CartId { get; set; }

        [ForeignKey("CartId")]
        public Cart? Cart { get; set; }

        [Column("prod_id")]
        public int ProdId { get; set; }

        [ForeignKey("ProdId")]
        public ProductMaster? Product { get; set; }

        [Required]
        [Column("quantity")]
        public int Quantity { get; set; }

        [Required]
        [Column("price_option")]
        public PriceOption PriceOption { get; set; }

        [Required]
        [Column("points_used")]
        public int PointsUsed { get; set; }

        [Column("added_at")]
        public DateTime AddedAt { get; set; } = DateTime.Now;
    }
}
