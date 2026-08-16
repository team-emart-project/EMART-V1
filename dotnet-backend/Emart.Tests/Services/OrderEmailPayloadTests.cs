using System;
using System.Collections.Generic;
using System.Text.Json;
using Emart.Api.DTOs;
using Emart.Api.Models;
using Emart.Api.Services;

namespace Emart.Tests.Services
{
    /// <summary>
    /// Guards the wire format of the call to backend-email-microservice.
    ///
    /// WHY THIS TEST EXISTS: the microservice is a Spring Boot app. Nothing in
    /// this solution compiles against its DTOs, so a renamed property or a
    /// PascalCase key would build cleanly here and arrive there as a null —
    /// producing an invoice with a blank order number, or a 400 nobody sees
    /// because the client swallows it. The assertions below are the contract.
    /// </summary>
    public class OrderEmailPayloadTests
    {
        private static OrderDto SampleOrder() => new()
        {
            OrderId = 41,
            OrderNo = "ORD-2026-048372",
            OrderDate = new DateTime(2026, 8, 10, 14, 22, 31, DateTimeKind.Local),
            CustomerName = "Rishiraj Chhalotre",
            MembershipNo = "EM-000123",
            Cardholder = true,
            SubtotalMrp = 4998.00m,
            SubtotalAmount = 3998.00m,
            TotalSavings = 1000.00m,
            TotalAmount = 3998.00m,
            PointsRedeemed = 0,
            PointsEarned = 119,
            PaymentStatus = "PENDING",
            OrderStatus = "PLACED",
            Items = new List<OrderDetailDto>
            {
                new()
                {
                    OrderDtlId = 1,
                    ProdId = 7,
                    ProdName = "Wireless Headphones",
                    Quantity = 2,
                    MrpPrice = 2499.00m,
                    CardholderPrice = 1999.00m,
                    PriceOption = PriceOption.HYBRID,
                    PriceCharged = 1999.00m,
                    LineTotal = 3998.00m,
                    LineSavings = 1000.00m,
                    PointsRedeemed = 60
                }
            },
            ShippingAddress = new AddressDto
            {
                AddressId = 3,
                AddressLine1 = "12 MG Road",
                City = "Indore",
                State = "Madhya Pradesh",
                ZipCode = "452001",
                Country = "India"
            }
        };

        [Test]
        public void From_CopiesEveryInvoiceFigure()
        {
            var payload = OrderEmailPayload.From(SampleOrder(), "rishiraj@example.com");

            Assert.Multiple(() =>
            {
                Assert.That(payload.SourceSystem, Is.EqualTo("DOTNET_BACKEND"));
                Assert.That(payload.EventType, Is.EqualTo("ORDER_PLACED"));

                Assert.That(payload.Customer.Email, Is.EqualTo("rishiraj@example.com"));
                Assert.That(payload.Customer.Name, Is.EqualTo("Rishiraj Chhalotre"));
                Assert.That(payload.Customer.Cardholder, Is.True);

                Assert.That(payload.Order.OrderNo, Is.EqualTo("ORD-2026-048372"));
                Assert.That(payload.Order.TotalAmount, Is.EqualTo(3998.00m));
                Assert.That(payload.Order.PointsEarned, Is.EqualTo(119));
                Assert.That(payload.Order.Items, Has.Count.EqualTo(1));
                Assert.That(payload.Order.Items[0].ProdName, Is.EqualTo("Wireless Headphones"));
                Assert.That(payload.Order.ShippingAddress!.City, Is.EqualTo("Indore"));

                // No billing address on the source order, so none on the payload.
                // The microservice treats it as optional and skips the block.
                Assert.That(payload.Order.BillingAddress, Is.Null);
            });
        }

        [Test]
        public void OrderDate_IsIsoLocalWithNoOffset()
        {
            var payload = OrderEmailPayload.From(SampleOrder(), "r@example.com");

            // java.time.LocalDateTime cannot parse "2026-08-10T14:22:31+05:30",
            // which is exactly what a Kind=Local DateTime serialises to by
            // default. Hence the explicit format in OrderEmailPayload.From.
            Assert.That(payload.Order.OrderDate, Is.EqualTo("2026-08-10T14:22:31"));
        }

        [Test]
        public void PriceOption_TravelsAsItsEnumName()
        {
            var payload = OrderEmailPayload.From(SampleOrder(), "r@example.com");

            // "HYBRID", not "3". The email service prints this string straight
            // onto the invoice line.
            Assert.That(payload.Order.Items[0].PriceOption, Is.EqualTo("HYBRID"));
        }

        [Test]
        public void SerialisedJson_UsesTheCamelCaseKeysJacksonExpects()
        {
            var payload = OrderEmailPayload.From(SampleOrder(), "rishiraj@example.com");

            string json = JsonSerializer.Serialize(payload, EmailServiceClient.JsonOptions);

            Assert.Multiple(() =>
            {
                Assert.That(json, Does.Contain("\"sourceSystem\":\"DOTNET_BACKEND\""));
                Assert.That(json, Does.Contain("\"orderNo\":\"ORD-2026-048372\""));
                Assert.That(json, Does.Contain("\"orderDate\":\"2026-08-10T14:22:31\""));
                Assert.That(json, Does.Contain("\"prodName\":\"Wireless Headphones\""));
                Assert.That(json, Does.Contain("\"priceOption\":\"HYBRID\""));
                Assert.That(json, Does.Contain("\"totalAmount\":3998.00"));
                Assert.That(json, Does.Contain("\"pointsEarned\":119"));
                Assert.That(json, Does.Contain("\"addressLine1\":\"12 MG Road\""));

                // PascalCase anywhere means the naming policy was lost.
                Assert.That(json, Does.Not.Contain("\"OrderNo\""));
                Assert.That(json, Does.Not.Contain("\"TotalAmount\""));
            });
        }
    }
}
