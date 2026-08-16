package com.example.demo.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * The one payload this service accepts, posted by whichever backend is live.
 *
 * THE CONTRACT IS THE PRODUCT. Both backends already compute every figure an
 * invoice needs, so this service asks for the finished numbers and recomputes
 * nothing — no prices, no totals, no points. If it did, the same order could
 * be summed one way on the website and another way in the customer's inbox.
 *
 * These are records on purpose: an inbound payload is read once and never
 * mutated, and Jackson + Bean Validation both work on record components.
 */
public record OrderEmailRequest(

        /**
         * Free text, for the log line only: "JAVA_BACKEND" or "DOTNET_BACKEND".
         * Since only one backend runs at a time, this is how you tell from the
         * service's own log which one produced a message.
         */
        String sourceSystem,

        /**
         * What happened. Only ORDER_PLACED is handled today; the field exists
         * so ORDER_SHIPPED or ORDER_CANCELLED can be added later without the
         * callers having to move to a different URL.
         */
        String eventType,

        @NotNull(message = "customer is required")
        @Valid
        CustomerDto customer,

        @NotNull(message = "order is required")
        @Valid
        OrderInvoiceDto order

) {
    public static final String EVENT_ORDER_PLACED = "ORDER_PLACED";

    /** Never null — an absent eventType means the original one. */
    public String eventTypeOrDefault() {
        return eventType == null || eventType.isBlank() ? EVENT_ORDER_PLACED : eventType;
    }

    public String sourceSystemOrUnknown() {
        return sourceSystem == null || sourceSystem.isBlank() ? "UNKNOWN" : sourceSystem;
    }
}
