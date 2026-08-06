using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace Emart.Api.Models
{
    [Table("category_master")]
    public class CategoryMaster
    {
        [Key]
        [Column("catmaster_id")]
        public int CatmasterId { get; set; }

        [Required]
        [Column("cat_id")]
        [MaxLength(3)]
        public string CatId { get; set; } = string.Empty;

        [Column("subcat_id")]
        [MaxLength(3)]
        public string? SubcatId { get; set; }

        [Required]
        [Column("cat_name")]
        [MaxLength(255)]
        public string CatName { get; set; } = string.Empty;

        [Column("cat_image_path")]
        [MaxLength(255)]
        public string? CatImagePath { get; set; }

        [Column("flag")]
        public bool Flag { get; set; }
    }
}
