using System;
using System.Collections.Generic;
using System.Globalization;
using System.IO;
using System.Linq;
using System.Text;
using Emart.Api.Models;

namespace Emart.Api.Services
{
    /// <summary>
    /// Writes a real, minimal PDF by hand.
    ///
    /// The previous version returned UTF-8 text under a application/pdf header,
    /// so the browser downloaded a file no PDF reader could open. Rather than
    /// pull in a full PDF library for one single-page invoice, this emits the
    /// smallest valid PDF 1.4 document: catalog, pages, one page, one Helvetica
    /// font, and a content stream of positioned text.
    ///
    /// The xref offsets have to be byte-exact or readers reject the file, which
    /// is why the objects are written into a byte buffer and their positions
    /// recorded as they go rather than assembled from a string at the end.
    /// </summary>
    public static class InvoicePdfGenerator
    {
        private const int PageWidth = 595;   // A4 at 72 dpi
        private const int PageHeight = 842;
        private const int LeftMargin = 56;

        public static byte[] Generate(Orders order)
        {
            var lines = BuildLines(order);
            string content = BuildContentStream(lines);
            return Assemble(content);
        }

        private static List<(string Text, int Size, bool Bold)> BuildLines(Orders order)
        {
            var inr = CultureInfo.GetCultureInfo("en-IN");
            string Money(decimal value) => value.ToString("N2", inr);

            var lines = new List<(string, int, bool)>
            {
                ("e-MART", 22, true),
                ("TAX INVOICE", 13, true),
                ("", 10, false),
                ($"Invoice for order  {order.OrderNo}", 11, false),
                ($"Order date         {order.OrderDate:dd MMM yyyy HH:mm}", 11, false),
                ($"Customer           {order.User?.FullName}", 11, false),
                ($"Membership no      {order.User?.MembershipNo}", 11, false),
                ("", 10, false)
            };

            if (order.ShippingAddress != null)
            {
                var a = order.ShippingAddress;
                lines.Add(("Ship to", 11, true));
                lines.Add(($"  {a.AddressLine1}", 10, false));
                if (!string.IsNullOrWhiteSpace(a.AddressLine2)) lines.Add(($"  {a.AddressLine2}", 10, false));
                lines.Add(($"  {a.City}, {a.State} {a.ZipCode}, {a.Country}", 10, false));
                lines.Add(("", 10, false));
            }

            lines.Add(("Items", 11, true));
            lines.Add(("  Qty  Description                              Option      Amount", 10, true));

            foreach (var item in order.Items)
            {
                string name = item.ProdNameSnapshot.Length > 38
                    ? item.ProdNameSnapshot[..35] + "..."
                    : item.ProdNameSnapshot;

                string amount = Money(item.PriceCharged * item.Quantity);
                lines.Add(($"  {item.Quantity,-4} {name,-40} {item.PriceOption,-11} {amount,10}", 10, false));

                if (item.PointsRedeemed > 0)
                {
                    lines.Add(($"       + {item.PointsRedeemed} e-Points redeemed", 9, false));
                }
            }

            decimal subtotalMrp = order.Items.Sum(i => i.MrpPrice * i.Quantity);

            lines.Add(("", 10, false));
            lines.Add(($"  Subtotal at MRP{Money(subtotalMrp),50}", 10, false));
            lines.Add(($"  You saved{Money(subtotalMrp - order.SubtotalAmount),56}", 10, false));
            // No tax anywhere in this project, so there is no tax line here
            // either — total is simply the subtotal.
            lines.Add(($"  TOTAL PAYABLE{Money(order.TotalAmount),52}", 12, true));
            lines.Add(("", 10, false));
            lines.Add(($"  e-Points redeemed  {order.PointsRedeemed}", 10, false));
            lines.Add(($"  e-Points earned    {order.PointsEarned}", 10, false));
            lines.Add(("", 10, false));
            lines.Add(($"  Payment status  {order.PaymentStatus}          Order status  {order.OrderStatus}", 10, false));
            lines.Add(("", 10, false));
            lines.Add(("This is a computer-generated invoice and needs no signature.", 9, false));

            return lines;
        }

        private static string BuildContentStream(List<(string Text, int Size, bool Bold)> lines)
        {
            var sb = new StringBuilder();
            int y = PageHeight - 64;

            foreach (var (text, size, bold) in lines)
            {
                if (!string.IsNullOrEmpty(text))
                {
                    sb.Append("BT /")
                      .Append(bold ? "F2" : "F1")
                      .Append(' ').Append(size).Append(" Tf ")
                      .Append(LeftMargin).Append(' ').Append(y).Append(" Td (")
                      .Append(EscapePdfText(text))
                      .Append(") Tj ET\n");
                }
                y -= size + 6;
                if (y < 48) break;   // one page only
            }

            return sb.ToString();
        }

        /// <summary>
        /// PDF string literals are Latin-1 and treat ( ) \ as syntax. A rupee
        /// sign or a smart quote in a product name would produce a corrupt file,
        /// so anything outside the printable ASCII range is replaced.
        /// </summary>
        private static string EscapePdfText(string text)
        {
            var sb = new StringBuilder(text.Length + 8);
            foreach (char c in text)
            {
                switch (c)
                {
                    case '(': sb.Append("\\("); break;
                    case ')': sb.Append("\\)"); break;
                    case '\\': sb.Append("\\\\"); break;
                    default:
                        sb.Append(c is >= ' ' and <= '~' ? c : '?');
                        break;
                }
            }
            return sb.ToString();
        }

        private static byte[] Assemble(string contentStream)
        {
            var buffer = new MemoryStream();
            var offsets = new List<int> { 0 };   // object 0 is the free head

            void Write(string s)
            {
                var bytes = Encoding.ASCII.GetBytes(s);
                buffer.Write(bytes, 0, bytes.Length);
            }

            void BeginObject(string body)
            {
                offsets.Add((int)buffer.Length);
                Write($"{offsets.Count - 1} 0 obj\n{body}\nendobj\n");
            }

            Write("%PDF-1.4\n");

            BeginObject("<< /Type /Catalog /Pages 2 0 R >>");
            BeginObject("<< /Type /Pages /Kids [3 0 R] /Count 1 >>");
            BeginObject($"<< /Type /Page /Parent 2 0 R /MediaBox [0 0 {PageWidth} {PageHeight}] " +
                        "/Resources << /Font << /F1 5 0 R /F2 6 0 R >> >> /Contents 4 0 R >>");

            var streamBytes = Encoding.ASCII.GetBytes(contentStream);
            BeginObject($"<< /Length {streamBytes.Length} >>\nstream\n{contentStream}endstream");

            BeginObject("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>");
            BeginObject("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold /Encoding /WinAnsiEncoding >>");

            int xrefOffset = (int)buffer.Length;
            Write($"xref\n0 {offsets.Count}\n");
            Write("0000000000 65535 f \n");
            for (int i = 1; i < offsets.Count; i++)
            {
                Write($"{offsets[i]:D10} 00000 n \n");
            }

            Write($"trailer\n<< /Size {offsets.Count} /Root 1 0 R >>\nstartxref\n{xrefOffset}\n%%EOF\n");

            return buffer.ToArray();
        }
    }
}
