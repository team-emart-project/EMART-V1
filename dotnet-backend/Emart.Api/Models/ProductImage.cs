using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace Emart.Api.Models
{
    [Table("product_image")]
    public class ProductImage
    {
        [Key]
        [Column("prod_image_id")]
        public int ProdImageId { get; set; }

        [Column("prod_id")]
        public int ProdId { get; set; }

        [ForeignKey("ProdId")]
        public ProductMaster? Product { get; set; }

        [Required]
        [Column("image_url")]
        [MaxLength(255)]
        public string ImageUrl { get; set; } = string.Empty;

        [Column("alt_text")]
        [MaxLength(100)]
        public string? AltText { get; set; }

        [Column("display_order")]
        public int DisplayOrder { get; set; }

        [Column("is_primary")]
        public bool IsPrimary { get; set; }
    }
}
