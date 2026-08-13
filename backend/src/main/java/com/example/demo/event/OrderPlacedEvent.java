package com.example.demo.event;

import com.example.demo.client.dto.OrderEmailPayload;

/**
 * Raised by OrderServiceImpl the moment an order is saved.
 *
 * IT CARRIES THE FINISHED PAYLOAD, not an order id. Two reasons:
 *
 *   1. The listener runs AFTER the transaction commits, and with
 *      spring.jpa.open-in-view=false there is no Hibernate session left. An id
 *      would force the listener to re-open one and re-read rows it already had.
 *
 *   2. A payload built inside the transaction is a snapshot of the order as it
 *      was placed. If anything edits that order a second later, the email still
 *      describes what the customer actually bought.
 */
public record OrderPlacedEvent(OrderEmailPayload payload) {

    public String orderNo() {
        return payload.order().orderNo();
    }
}
