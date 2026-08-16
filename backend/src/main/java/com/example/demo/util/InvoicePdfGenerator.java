package com.example.demo.util;

import com.example.demo.entity.Address;
import com.example.demo.entity.OrderDetail;
import com.example.demo.entity.Orders;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

/**
 * Renders an invoice as a PDF using OpenPDF (the maintained LGPL fork of
 * iText 2 — no commercial licence needed, unlike iText 5+/7).
 *
 * The BRD marks "Print Invoice" as mandatory. In the target architecture this
 * belongs to the separate Notification/PDF microservice; generating it in
 * process keeps the feature working until that service exists, and the calling
 * code (OrderService) would not change if it moved.
 */
@Component
public class InvoicePdfGenerator {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

    private static final Font TITLE   = new Font(Font.HELVETICA, 20, Font.BOLD, new Color(47, 84, 150));
    private static final Font HEADING = new Font(Font.HELVETICA, 11, Font.BOLD, Color.WHITE);
    private static final Font LABEL   = new Font(Font.HELVETICA, 10, Font.BOLD);
    private static final Font NORMAL  = new Font(Font.HELVETICA, 10);
    private static final Font SMALL   = new Font(Font.HELVETICA, 8, Font.ITALIC, Color.GRAY);
    private static final Color HEADER_BG = new Color(47, 84, 150);

    public byte[] generate(Orders order) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 36, 36, 40, 40);

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            document.add(paragraph("e-MART", TITLE));
            document.add(paragraph("TAX INVOICE", LABEL));
            document.add(Chunk.NEWLINE);

            document.add(meta(order));
            document.add(Chunk.NEWLINE);

            document.add(addresses(order));
            document.add(Chunk.NEWLINE);

            document.add(itemsTable(order));
            document.add(Chunk.NEWLINE);

            document.add(totals(order));
            document.add(Chunk.NEWLINE);

            document.add(paragraph(
                    "This is a computer generated invoice and does not require a signature.", SMALL));

        } catch (DocumentException ex) {
            // Wrapped as unchecked: the caller cannot meaningfully recover, and
            // GlobalExceptionHandler turns it into a clean 500.
            throw new IllegalStateException("Failed to generate invoice PDF", ex);
        } finally {
            if (document.isOpen()) {
                document.close();
            }
        }
        return out.toByteArray();
    }

    // ------------------------------------------------------------------

    private Paragraph paragraph(String text, Font font) {
        return new Paragraph(text, font);
    }

    private PdfPTable meta(Orders order) {
        PdfPTable t = new PdfPTable(2);
        t.setWidthPercentage(100);
        t.getDefaultCell().setBorder(Rectangle.NO_BORDER);

        t.addCell(cell("Invoice No: " + order.getOrderNo(), LABEL));
        t.addCell(cell("Date: " + order.getOrderDate().format(DATE_FMT), NORMAL));
        t.addCell(cell("Customer: " + fullName(order), NORMAL));
        t.addCell(cell("Membership No: " + order.getUser().getMembershipNo(), NORMAL));
        t.addCell(cell("Order Status: " + order.getOrderStatus().name(), NORMAL));
        t.addCell(cell("Payment Status: " + order.getPaymentStatus().name(), NORMAL));
        return t;
    }

    private PdfPTable addresses(Orders order) {
        PdfPTable t = new PdfPTable(2);
        t.setWidthPercentage(100);
        t.getDefaultCell().setBorder(Rectangle.NO_BORDER);

        t.addCell(cell("Bill To:\n" + format(order.getBillingAddress()), NORMAL));
        t.addCell(cell("Ship To:\n" + format(order.getShippingAddress()), NORMAL));
        return t;
    }

    private PdfPTable itemsTable(Orders order) throws DocumentException {
        PdfPTable t = new PdfPTable(new float[]{4f, 1f, 1.6f, 1.6f, 1.8f});
        t.setWidthPercentage(100);

        for (String h : new String[]{"Product", "Qty", "MRP", "Price", "Line Total"}) {
            PdfPCell c = new PdfPCell(new Phrase(h, HEADING));
            c.setBackgroundColor(HEADER_BG);
            c.setPadding(6);
            t.addCell(c);
        }

        for (OrderDetail item : order.getItems()) {
            BigDecimal lineTotal = item.getPriceCharged()
                    .multiply(BigDecimal.valueOf(item.getQuantity()));

            t.addCell(bodyCell(item.getProdNameSnapshot(), Element.ALIGN_LEFT));
            t.addCell(bodyCell(String.valueOf(item.getQuantity()), Element.ALIGN_CENTER));
            t.addCell(bodyCell(money(item.getMrpPrice()), Element.ALIGN_RIGHT));
            t.addCell(bodyCell(money(item.getPriceCharged()), Element.ALIGN_RIGHT));
            t.addCell(bodyCell(money(lineTotal), Element.ALIGN_RIGHT));
        }
        return t;
    }

    private PdfPTable totals(Orders order) {
        PdfPTable t = new PdfPTable(new float[]{3f, 1.5f});
        t.setWidthPercentage(45);
        t.setHorizontalAlignment(Element.ALIGN_RIGHT);

        // No tax line: the customer is charged exactly the price shown on the
        // product card, so subtotal and total are the same number.
        addTotal(t, "Subtotal", money(order.getSubtotalAmount()), false);

        // Points are shown for the record, not subtracted — a line paid in
        // points already has a cash price of zero in the subtotal above.
        if (order.getPointsRedeemed() != null && order.getPointsRedeemed() > 0) {
            addTotal(t, "e-Points spent", String.valueOf(order.getPointsRedeemed()), false);
        }
        addTotal(t, "Total Payable", money(order.getTotalAmount()), true);

        if (order.getPointsEarned() != null && order.getPointsEarned() > 0) {
            addTotal(t, "e-Points earned", "+" + order.getPointsEarned(), false);
        }
        return t;
    }

    private void addTotal(PdfPTable t, String label, String value, boolean bold) {
        Font f = bold ? LABEL : NORMAL;
        PdfPCell l = new PdfPCell(new Phrase(label, f));
        PdfPCell v = new PdfPCell(new Phrase(value, f));
        l.setBorder(Rectangle.NO_BORDER);
        v.setBorder(Rectangle.NO_BORDER);
        v.setHorizontalAlignment(Element.ALIGN_RIGHT);
        l.setPadding(4);
        v.setPadding(4);
        t.addCell(l);
        t.addCell(v);
    }

    private PdfPCell bodyCell(String text, int align) {
        PdfPCell c = new PdfPCell(new Phrase(text, NORMAL));
        c.setPadding(5);
        c.setHorizontalAlignment(align);
        return c;
    }

    private PdfPCell cell(String text, Font font) {
        PdfPCell c = new PdfPCell(new Phrase(text, font));
        c.setBorder(Rectangle.NO_BORDER);
        c.setPadding(3);
        return c;
    }

    private String fullName(Orders order) {
        String last = order.getUser().getLastName();
        return order.getUser().getFirstName() + (last == null ? "" : " " + last);
    }

    private String format(Address a) {
        StringBuilder sb = new StringBuilder();
        sb.append(a.getAddressLine1());
        if (a.getAddressLine2() != null && !a.getAddressLine2().isBlank()) {
            sb.append("\n").append(a.getAddressLine2());
        }
        sb.append("\n").append(a.getCity()).append(", ").append(a.getState())
          .append(" ").append(a.getZipCode())
          .append("\n").append(a.getCountry());
        return sb.toString();
    }

    /** Rs. rather than the rupee symbol: the built-in PDF fonts cannot render it. */
    private String money(BigDecimal amount) {
        return "Rs. " + (amount == null ? "0.00" : amount.toPlainString());
    }
}
