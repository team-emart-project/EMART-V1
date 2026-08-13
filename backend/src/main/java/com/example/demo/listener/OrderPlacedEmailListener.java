package com.example.demo.listener;

import com.example.demo.client.EmailServiceClient;
import com.example.demo.event.OrderPlacedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * The seam between "an order was placed" and "an email should go out".
 *
 * OrderServiceImpl publishes an event and knows nothing else — no HTTP client,
 * no URL, no retry policy. Deleting this one class and the client turns the
 * email feature off completely without touching a line of order logic, which
 * is the decoupling the brief asks for, enforced by the code rather than
 * promised in a comment.
 *
 * TWO ANNOTATIONS, TWO DIFFERENT JOBS:
 *
 *   @TransactionalEventListener(AFTER_COMMIT) — do not send until the order row
 *   is actually in MySQL. A plain @EventListener runs INSIDE the transaction,
 *   so a rollback further down placeOrder() would leave the customer holding a
 *   confirmation for an order that does not exist.
 *
 *   @Async — do not make the shopper wait for it. Without this, AFTER_COMMIT
 *   still runs on the checkout thread and every HTTP timeout is added to the
 *   response time of /api/orders/checkout.
 */
@Component
public class OrderPlacedEmailListener {

    private static final Logger log = LoggerFactory.getLogger(OrderPlacedEmailListener.class);

    private final EmailServiceClient emailServiceClient;

    public OrderPlacedEmailListener(EmailServiceClient emailServiceClient) {
        this.emailServiceClient = emailServiceClient;
    }

    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderPlaced(OrderPlacedEvent event) {
        try {
            emailServiceClient.sendOrderPlaced(event.payload());
        } catch (RuntimeException ex) {
            // EmailServiceClient already swallows the expected failures. This
            // is the backstop for the unexpected ones: nothing thrown on this
            // thread can reach a user, so an uncaught exception here would
            // just vanish into the executor.
            log.error("Unexpected failure notifying the email service for orderNo={}",
                    event.orderNo(), ex);
        }
    }
}
