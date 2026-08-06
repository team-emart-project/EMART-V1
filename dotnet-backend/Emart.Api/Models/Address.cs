using System;
using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace Emart.Api.Models
{
    [Table("address")]
    public class Address
    {
        [Key]
        [Column("address_id")]
        public int AddressId { get; set; }

        [Column("user_id")]
        public int UserId { get; set; }

        [ForeignKey("UserId")]
        public User? User { get; set; }

        [Required]
        [Column("address_line1")]
        [MaxLength(255)]
        public string AddressLine1 { get; set; } = string.Empty;

        [Column("address_line2")]
        [MaxLength(255)]
        public string? AddressLine2 { get; set; }

        [Required]
        [Column("city")]
        [MaxLength(100)]
        public string City { get; set; } = string.Empty;

        [Required]
        [Column("state")]
        [MaxLength(100)]
        public string State { get; set; } = string.Empty;

        [Required]
        [Column("zip_code")]
        [MaxLength(20)]
        public string ZipCode { get; set; } = string.Empty;

        [Required]
        [Column("country")]
        [MaxLength(100)]
        public string Country { get; set; } = "India";

        [Required]
        [Column("address_type")]
        public AddressType AddressType { get; set; } = AddressType.SHIPPING;

        [Column("is_default")]
        public bool IsDefault { get; set; }

        [Column("created_at")]
        public DateTime CreatedAt { get; set; } = DateTime.Now;
    }
}
