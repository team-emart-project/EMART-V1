package com.example.demo.service;

import com.example.demo.config.EmailProperties;
import com.example.demo.dto.request.AddressDto;
import com.example.demo.dto.request.CustomerDto;
import com.example.demo.dto.request.OrderEmailRequest;
import com.example.demo.dto.request.OrderInvoiceDto;
import com.example.demo.dto.request.OrderItemDto;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Builds the two bodies of the message: an HTML invoice and a plain-text
 * fallback.
 *
 * ALL FORMATTING HAPPENS HERE, not in the template. The template receives
 * strings that are already money-formatted and date-formatted, which keeps it
 * to markup and one th:each — and, more practically, avoids leaning on
 * Thymeleaf's SpEL to read Java records, whose accessors are prodName() rather
 * than getProdName().
 */
@Component
public class InvoiceRenderer {

    /**
     * Fixed, not the server's default locale: an invoice must read the same
     * whichever machine sent it. (Java's en-IN still groups in thousands, so
     * this prints 100,000.00 rather than the Indian 1,00,000.00 — changing
     * that would mean a custom DecimalFormat, which is not worth it here.)
     */
    private static final Locale MONEY_LOCALE = Locale.forLanguageTag("en-IN");

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a", Locale.ENGLISH);

    private static final String TEMPLATE = "email/order-placed";

    private final TemplateEngine templateEngine;
    private final EmailProperties properties;

    public InvoiceRenderer(TemplateEngine templateEngine, EmailProperties properties) {
        this.templateEngine = templateEngine;
        this.properties = properties;
    }

    /** "[e-MART] Order Placed - ORD-2026-048372" */
    public String buildSubject(OrderInvoiceDto order) {
        String prefix = properties.getSubjectPrefix();
        String head = (prefix == null || prefix.isBlank()) ? "" : prefix.trim() + " ";
        return head + "Order Placed - " + order.orderNo();
    }

    public String renderHtml(OrderEmailRequest request) {
        Context context = new Context();
        context.setVariables(buildModel(request));
        return templateEngine.process(TEMPLATE, context);
    }

    /**
     * The text/plain alternative.
     *
     * Not decoration: a message with an HTML part and no text part scores worse
     * with spam filters, and some corporate mail clients still show the text
     * one. It is generated from the same data, so the two can never disagree.
     */
    public String renderText(OrderEmailRequest request) {
        CustomerDto customer = request.customer();
        OrderInvoiceDto order = request.order();

        StringBuilder sb = new StringBuilder();
        sb.append(properties.getStoreName()).append(" - ORDER PLACED\n");
        sb.append("=".repeat(52)).append("\n\n");
        sb.append("Hi ").append(customer.firstName()).append(",\n\n");
        sb.append("Thanks for your order. Here is your invoice.\n\n");
        sb.append("Order No   : ").append(order.orderNo()).append('\n');
        sb.append("Order Date : ").append(DATE_FORMAT.format(order.orderDateOrNow())).append('\n');
        sb.append("Status     : ").append(nvl(order.orderStatus(), "PLACED"))
          .append("  |  Payment: ").append(nvl(order.paymentStatus(), "PENDING")).append("\n\n");

        sb.append("ITEMS\n").append("-".repeat(52)).append('\n');
        for (OrderItemDto item : order.items()) {
            sb.append("- ").append(item.prodName())
              .append("  x").append(item.quantity())
              .append("  ").append(money(item.lineTotalOrDerived()));
            if (item.pointsRedeemedOrZero() > 0) {
                sb.append("  (+").append(item.pointsRedeemedOrZero()).append(" e-Points)");
            }
            sb.append('\n');
        }

        sb.append("-".repeat(52)).append('\n');
        sb.append("Subtotal   : ").append(money(order.subtotalAmount())).append('\n');
        if (isPositive(order.totalSavingsOrDerived())) {
            sb.append("You saved  : ").append(money(order.totalSavingsOrDerived())).append('\n');
        }
        if (order.pointsRedeemedOrZero() > 0) {
            sb.append("Points used: ").append(order.pointsRedeemedOrZero()).append('\n');
        }
        sb.append("TOTAL      : ").append(money(order.totalAmount())).append('\n');
        if (order.pointsEarnedOrZero() > 0) {
            sb.append("\nYou earned ").append(order.pointsEarnedOrZero())
              .append(" e-Points on this order.\n");
        }

        if (order.shippingAddress() != null) {
            sb.append("\nDELIVERING TO\n").append(order.shippingAddress().singleLine()).append('\n');
        }

        sb.append("\nQuestions? Reply to ").append(properties.getSupportEmail()).append(".\n");
        sb.append("This is an automated message from ").append(properties.getStoreName()).append(".\n");
        return sb.toString();
    }

    // ------------------------------------------------------------------
    // Model
    // ------------------------------------------------------------------

    private Map<String, Object> buildModel(OrderEmailRequest request) {
        CustomerDto customer = request.customer();
        OrderInvoiceDto order = request.order();

        Map<String, Object> model = new LinkedHashMap<>();
        model.put("storeName", properties.getStoreName());
        model.put("supportEmail", properties.getSupportEmail());

        model.put("firstName", customer.firstName());
        model.put("customerName", customer.name());
        model.put("membershipNo", customer.membershipNo());
        model.put("cardholder", customer.isCardholder());

        model.put("orderNo", order.orderNo());
        model.put("orderDate", DATE_FORMAT.format(order.orderDateOrNow()));
        model.put("orderStatus", nvl(order.orderStatus(), "PLACED"));
        model.put("paymentStatus", nvl(order.paymentStatus(), "PENDING"));

        model.put("items", buildRows(order.items()));

        model.put("subtotalMrp", money(order.subtotalMrp()));
        model.put("subtotalAmount", money(order.subtotalAmount()));
        model.put("totalSavings", money(order.totalSavingsOrDerived()));
        model.put("hasSavings", isPositive(order.totalSavingsOrDerived()));
        model.put("totalAmount", money(order.totalAmount()));

        model.put("pointsRedeemed", order.pointsRedeemedOrZero());
        model.put("hasPointsRedeemed", order.pointsRedeemedOrZero() > 0);
        model.put("pointsEarned", order.pointsEarnedOrZero());
        model.put("hasPointsEarned", order.pointsEarnedOrZero() > 0);

        model.put("shippingAddress", addressLine(order.shippingAddress()));
        model.put("billingAddress", addressLine(order.billingAddress()));

        return model;
    }

    private List<Map<String, Object>> buildRows(List<OrderItemDto> items) {
        List<Map<String, Object>> rows = new ArrayList<>(items.size());

        for (OrderItemDto item : items) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("name", item.prodName());
            row.put("quantity", item.quantity());
            row.put("mrp", money(item.mrpPrice()));
            row.put("unitPrice", money(item.priceCharged()));
            row.put("lineTotal", money(item.lineTotalOrDerived()));
            row.put("priceOption", nvl(item.priceOption(), "REGULAR"));

            // Only worth showing the struck-through MRP when it differs from
            // what was actually charged, otherwise every row looks discounted.
            row.put("discounted", item.mrpPrice() != null && item.priceCharged() != null
                    && item.mrpPrice().compareTo(item.priceCharged()) > 0);

            row.put("points", item.pointsRedeemedOrZero());
            row.put("hasPoints", item.pointsRedeemedOrZero() > 0);
            rows.add(row);
        }
        return rows;
    }

    private String addressLine(AddressDto address) {
        if (address == null) return null;
        String line = address.singleLine();
        return line.isBlank() ? null : line;
    }

    // ------------------------------------------------------------------
    // Formatting
    // ------------------------------------------------------------------

    private String money(BigDecimal value) {
        BigDecimal amount = value == null ? BigDecimal.ZERO : value;
        NumberFormat format = NumberFormat.getNumberInstance(MONEY_LOCALE);
        format.setMinimumFractionDigits(2);
        format.setMaximumFractionDigits(2);
        return properties.getCurrencySymbol() + format.format(amount);
    }

    private static boolean isPositive(BigDecimal value) {
        return value != null && value.signum() > 0;
    }

    private static String nvl(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
