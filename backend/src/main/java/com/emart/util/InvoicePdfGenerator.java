package com.emart.util;

import java.io.ByteArrayOutputStream;

import org.springframework.stereotype.Component;

import com.emart.entity.OrderDetail;
import com.emart.entity.Orders;
import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;

@Component
public class InvoicePdfGenerator {

    public byte[] generate(Orders order) {

        try {

            ByteArrayOutputStream out = new ByteArrayOutputStream();

            Document document = new Document();

            PdfWriter.getInstance(document, out);

            document.open();

            document.add(new Paragraph("E-Mart Invoice"));

            document.add(new Paragraph("----------------------------"));

            document.add(new Paragraph("Order No : " + order.getOrderNo()));

            document.add(new Paragraph("Order Date : " + order.getOrderDate()));

            document.add(new Paragraph("Total Amount : ₹" + order.getTotalAmount()));

            document.add(new Paragraph(" "));

            document.add(new Paragraph("Items"));

            for (OrderDetail item : order.getItems()) {

                document.add(new Paragraph(

                        item.getProdNameSnapshot()

                        + "   Qty : "

                        + item.getQuantity()

                        + "   Price : "

                        + item.getPriceCharged()

                ));
            }

            document.close();

            return out.toByteArray();

        } catch (Exception e) {

            throw new RuntimeException("Unable to generate invoice PDF", e);

        }
    }

}