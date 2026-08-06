using System;
using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace Emart.Api.Models
{
    [Table("users")]
    public class User
    {
        [Key]
        [Column("user_id")]
        public int UserId { get; set; }

        [Column("membership_no")]
        [MaxLength(20)]
        public string MembershipNo { get; set; } = string.Empty;

        [Required]
        [Column("first_name")]
        [MaxLength(100)]
        public string FirstName { get; set; } = string.Empty;

        [Column("last_name")]
        [MaxLength(100)]
        public string? LastName { get; set; }

        [Required]
        [Column("email")]
        [MaxLength(150)]
        public string Email { get; set; } = string.Empty;

        // NULL for a Google-only account. A fake hash would look like a usable
        // credential to any code that later reads this column.
        [Column("password_hash")]
        [MaxLength(255)]
        public string? PasswordHash { get; set; }

        [Required]
        [Column("auth_provider")]
        public AuthProvider AuthProvider { get; set; } = AuthProvider.LOCAL;

        [Column("google_sub")]
        [MaxLength(64)]
        public string? GoogleSub { get; set; }

        [Column("profile_image_url")]
        [MaxLength(500)]
        public string? ProfileImageUrl { get; set; }

        [Column("reset_password_token")]
        [MaxLength(255)]
        public string? ResetPasswordToken { get; set; }

        [Column("reset_password_token_expiry")]
        public DateTime? ResetPasswordTokenExpiry { get; set; }

        [Column("phone")]
        [MaxLength(20)]
        public string? Phone { get; set; }

        [Column("dob")]
        public DateTime? Dob { get; set; }

        [Column("gender")]
        [MaxLength(10)]
        public string? Gender { get; set; }

        [Column("education")]
        [MaxLength(100)]
        public string? Education { get; set; }

        [Column("occupation")]
        [MaxLength(100)]
        public string? Occupation { get; set; }

        [Column("annual_income")]
        public decimal? AnnualIncome { get; set; }

        [Column("marketing_consent")]
        public bool MarketingConsent { get; set; }

        [Required]
        [Column("role")]
        public RoleType Role { get; set; } = RoleType.CUSTOMER;

        [Column("is_cardholder")]
        public bool IsCardholder { get; set; }

        [Column("is_active")]
        public bool IsActive { get; set; } = true;

        [Column("created_at")]
        public DateTime CreatedAt { get; set; } = DateTime.Now;

        [Column("updated_at")]
        public DateTime? UpdatedAt { get; set; }

        [NotMapped]
        public string FullName => string.IsNullOrWhiteSpace(LastName) ? FirstName : $"{FirstName} {LastName}";
    }
}
