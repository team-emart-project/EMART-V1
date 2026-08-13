using System;
using System.Collections.Generic;
using System.Globalization;
using System.Linq;

namespace Emart.Api.DTOs
{
    /// <summary>
    /// The request body of POST /api/send-order-email on
    /// backend-email-microservice.
    ///
    /// THE SAME CONTRACT THE JAVA BACKEND POSTS. The microservice has one
    /// endpoint and one payload shape, and only one backend runs at a time, so
    /// these records mirror backend/src/main/java/com/example/demo/client/dto/
    /// OrderEmailPayload.java field for field. Rename anything here and the
    /// email service silently receives a null.
    ///
    /// Note the property names are PascalCase, as C# wants; EmailServiceClient
    /// serialises with a camelCase policy so the JSON on the wire is
    /// orderNo/totalAmount, which is what Jackson expects on the other end.
    /// </summary>
    public class OrderEmailPayload
    {
        public const string SourceDotnetBackend = "DOTNET_BACKEND";
        public const string EventOrderPlaced = "ORDER_PLACED";

        public string SourceSystem { get; set; } = SourceDotnetBackend;
        public string EventType { get; set; } = EventOrderPlaced;

        public OrderEmailCustomer Customer { get; set; } = new();
        public OrderEmailOrder Order { get; set; } = new();

        /// <summary>
        /// Builds the payload from the OrderDto the checkout already produced.
        /// </summary>
        /// <param name="customerEmail">
        /// From the User row. OrderDto deliberately does not carry it — the
        /// frontend has no use for the address of the person already signed in.
        /// </param>
        public static OrderEmailPayload From(OrderDto order, string customerEmail)
        {
            return new OrderEmailPayload
            {
                Customer = new OrderEmailCustomer
                {
                    Name = order.CustomerName,
                    Email = customerEmail,
                    MembershipNo = order.MembershipNo,
                    Cardholder = order.Cardholder
                },
                Order = new OrderEmailOrder
                {
                    OrderId = order.OrderId,
                    OrderNo = order.OrderNo,

                    // Formatted by hand as ISO LOCAL date-time, deliberately.
                    // A DateTime with Kind=Local serialises with an offset
                    // ("+05:30"), and the microservice binds this field to a
                    // java.time.LocalDateTime, which refuses to parse one.
                    OrderDate = order.OrderDate.ToString("yyyy-MM-dd'T'HH:mm:ss",
                                                         CultureInfo.InvariantCulture),

                    OrderStatus = order.OrderStatus,
                    PaymentStatus = order.PaymentStatus,
                    SubtotalMrp = order.SubtotalMrp,
                    SubtotalAmount = order.SubtotalAmount,
                    TotalSavings = order.TotalSavings,
                    TotalAmount = order.TotalAmount,
                    PointsRedeemed = order.PointsRedeemed,
                    PointsEarned = order.PointsEarned,
                    Items = order.Items.Select(ToItem).ToList(),
                    ShippingAddress = ToAddress(order.ShippingAddress),
                    BillingAddress = ToAddress(order.BillingAddress)
                }
            };
        }

        private static OrderEmailItem ToItem(OrderDetailDto item) => new()
        {
            ProdId = item.ProdId,
            ProdName = item.ProdName,
            Quantity = item.Quantity,
            MrpPrice = item.MrpPrice,
            CardholderPrice = item.CardholderPrice,
            // The enum travels as its NAME ("HYBRID"), so the email service
            // never has to keep a PriceOption of its own in step with two
            // backends that both already have one.
            PriceOption = item.PriceOption.ToString(),
            PriceCharged = item.PriceCharged,
            LineTotal = item.LineTotal,
            LineSavings = item.LineSavings,
            PointsRedeemed = item.PointsRedeemed
        };

        private static OrderEmailAddress? ToAddress(AddressDto? address)
        {
            if (address == null) return null;

            return new OrderEmailAddress
            {
                AddressLine1 = address.AddressLine1,
                AddressLine2 = address.AddressLine2,
                City = address.City,
                State = address.State,
                ZipCode = address.ZipCode,
                Country = address.Country
            };
        }
    }

    public class OrderEmailCustomer
    {
        public string Name { get; set; } = string.Empty;
        public string Email { get; set; } = string.Empty;
        public string? MembershipNo { get; set; }
        public bool Cardholder { get; set; }
    }

    public class OrderEmailOrder
    {
        public int? OrderId { get; set; }
        public string? OrderNo { get; set; }

        /// <summary>ISO local date-time, no offset. See OrderEmailPayload.From.</summary>
        public string OrderDate { get; set; } = string.Empty;

        public string OrderStatus { get; set; } = string.Empty;
        public string PaymentStatus { get; set; } = string.Empty;

        public decimal SubtotalMrp { get; set; }
        public decimal SubtotalAmount { get; set; }
        public decimal TotalSavings { get; set; }
        public decimal TotalAmount { get; set; }

        public int PointsRedeemed { get; set; }
        public int PointsEarned { get; set; }

        public List<OrderEmailItem> Items { get; set; } = new();

        public OrderEmailAddress? ShippingAddress { get; set; }
        public OrderEmailAddress? BillingAddress { get; set; }
    }

    public class OrderEmailItem
    {
        public int ProdId { get; set; }
        public string ProdName { get; set; } = string.Empty;
        public int Quantity { get; set; }
        public decimal MrpPrice { get; set; }
        public decimal? CardholderPrice { get; set; }
        public string PriceOption { get; set; } = string.Empty;
        public decimal PriceCharged { get; set; }
        public decimal LineTotal { get; set; }
        public decimal LineSavings { get; set; }
        public int PointsRedeemed { get; set; }
    }

    public class OrderEmailAddress
    {
        public string? AddressLine1 { get; set; }
        public string? AddressLine2 { get; set; }
        public string? City { get; set; }
        public string? State { get; set; }
        public string? ZipCode { get; set; }
        public string? Country { get; set; }
    }
}
