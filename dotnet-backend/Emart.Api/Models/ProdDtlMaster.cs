using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace Emart.Api.Models
{
    [Table("prod_dtl_master")]
    public class ProdDtlMaster
    {
        [Key]
        [Column("prod_dtl_id")]
        public int ProdDtlId { get; set; }

        [Column("prod_id")]
        public int ProdId { get; set; }

        [ForeignKey("ProdId")]
        public ProductMaster? Product { get; set; }

        [Column("config_id")]
        public int ConfigId { get; set; }

        [ForeignKey("ConfigId")]
        public ConfigMaster? Config { get; set; }

        [Required]
        [Column("config_dtls")]
        public string ConfigDtls { get; set; } = string.Empty;
    }
}
