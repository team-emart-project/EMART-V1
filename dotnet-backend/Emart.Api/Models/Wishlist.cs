using System;
using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace Emart.Api.Models
{
    [Table("wishlist")]
    public class Wishlist
    {
        [Key]
        [Column("wishlist_id")]
        public int WishlistId { get; set; }

        [Column("user_id")]
        public int UserId { get; set; }

        [ForeignKey("UserId")]
        public User? User { get; set; }

        [Column("prod_id")]
        public int ProdId { get; set; }

        [ForeignKey("ProdId")]
        public ProductMaster? Product { get; set; }

        [Column("added_at")]
        public DateTime AddedAt { get; set; } = DateTime.Now;
    }
}
