using System;
using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace Emart.Api.Models
{
    [Table("emart_card")]
    public class EmartCard
    {
        [Key]
        [Column("card_id")]
        public int CardId { get; set; }

        [Column("user_id")]
        public int UserId { get; set; }

        [ForeignKey("UserId")]
        public User? User { get; set; }

        [Required]
        [Column("card_number")]
        [MaxLength(30)]
        public string CardNumber { get; set; } = string.Empty;

        // DATE columns, not DATETIME — the schema stores the day only.
        [Column("application_date")]
        public DateTime ApplicationDate { get; set; }

        [Column("approval_date")]
        public DateTime? ApprovalDate { get; set; }

        [Required]
        [Column("status")]
        public CardStatus Status { get; set; } = CardStatus.PENDING;

        [Column("points_balance")]
        public int PointsBalance { get; set; }

        [Column("employment_details")]
        [MaxLength(255)]
        public string? EmploymentDetails { get; set; }

        [Column("bank_account_no")]
        [MaxLength(30)]
        public string? BankAccountNo { get; set; }

        [Column("pan_number")]
        [MaxLength(20)]
        public string? PanNumber { get; set; }

        [Column("created_at")]
        public DateTime CreatedAt { get; set; } = DateTime.Now;
    }
}
