using System;
using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace Emart.Api.Models
{
    [Table("payment")]
    public class Payment
    {
        [Key]
        [Column("payment_id")]
        public int PaymentId { get; set; }

        [Column("order_id")]
        public int OrderId { get; set; }

        [ForeignKey("OrderId")]
        public Orders? Order { get; set; }

        [Required]
        [Column("payment_method")]
        [MaxLength(30)]
        public string PaymentMethod { get; set; } = "CARD";

        // Only ever the last four digits. The full number and the CVV are never
        // stored anywhere, which is why there is no column for either.
        [Column("card_last4")]
        [MaxLength(4)]
        public string? CardLast4 { get; set; }

        [Column("amount")]
        public decimal Amount { get; set; }

        [Required]
        [Column("status")]
        public PaymentStatus Status { get; set; } = PaymentStatus.PENDING;

        [Column("transaction_ref")]
        [MaxLength(100)]
        public string? TransactionRef { get; set; }

        [Column("transaction_date")]
        public DateTime TransactionDate { get; set; } = DateTime.Now;
    }
}
