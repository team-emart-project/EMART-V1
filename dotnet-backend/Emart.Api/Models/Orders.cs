using System;
using System.Collections.Generic;
using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace Emart.Api.Models
{
    [Table("orders")]
    public class Orders
    {
        [Key]
        [Column("order_id")]
        public int OrderId { get; set; }

        [Required]
        [Column("order_no")]
        [MaxLength(50)]
        public string OrderNo { get; set; } = string.Empty;

        [Column("user_id")]
        public int UserId { get; set; }

        [ForeignKey("UserId")]
        public User? User { get; set; }

        [Column("shipping_address_id")]
        public int ShippingAddressId { get; set; }

        [ForeignKey("ShippingAddressId")]
        public Address? ShippingAddress { get; set; }

        [Column("billing_address_id")]
        public int BillingAddressId { get; set; }

        [ForeignKey("BillingAddressId")]
        public Address? BillingAddress { get; set; }

        [Column("order_date")]
        public DateTime OrderDate { get; set; } = DateTime.Now;

        [Column("subtotal_amount")]
        public decimal SubtotalAmount { get; set; }

        [Column("total_amount")]
        public decimal TotalAmount { get; set; }

        [Column("points_redeemed")]
        public int PointsRedeemed { get; set; }

        [Column("points_earned")]
        public int PointsEarned { get; set; }

        [Required]
        [Column("payment_status")]
        public PaymentStatus PaymentStatus { get; set; } = PaymentStatus.PENDING;

        [Required]
        [Column("order_status")]
        public OrderStatus OrderStatus { get; set; } = OrderStatus.PLACED;

        public List<OrderDetail> Items { get; set; } = new List<OrderDetail>();

        [Column("created_at")]
        public DateTime CreatedAt { get; set; } = DateTime.Now;

        [Column("updated_at")]
        public DateTime? UpdatedAt { get; set; }
    }
}
