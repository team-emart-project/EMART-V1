package com.example.demo.service.interfaces;

import com.example.demo.dto.request.OrderEmailRequest;
import com.example.demo.dto.response.EmailSendResponse;

/** The whole public surface of this service, in one method. */
public interface OrderEmailService {

    /**
     * Accepts an order-placed notification and hands it to the mail thread.
     *
     * Returns as soon as the payload is on the queue — the returned status is
     * ACCEPTED, never SENT. Whether the mail server took it is reported in the
     * log under the same requestId.
     */
    EmailSendResponse acceptOrderPlaced(OrderEmailRequest request);
}
