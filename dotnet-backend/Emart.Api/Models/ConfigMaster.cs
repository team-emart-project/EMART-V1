using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace Emart.Api.Models
{
    [Table("config_master")]
    public class ConfigMaster
    {
        [Key]
        [Column("config_id")]
        public int ConfigId { get; set; }

        [Required]
        [Column("config_name")]
        [MaxLength(100)]
        public string ConfigName { get; set; } = string.Empty;
    }
}
