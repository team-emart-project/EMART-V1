package com.example.demo.service.implementation;

import com.example.demo.config.AsyncConfig;
import com.example.demo.config.EmailProperties;
import com.example.demo.dto.request.OrderEmailRequest;
import com.example.demo.exception.EmailDeliveryException;
import com.example.demo.service.InvoiceRenderer;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

/**
 * Renders and sends. Runs on the pool from {@link AsyncConfig}, never on the
 * HTTP thread.
 *
 * SEPARATE BEAN ON PURPOSE. @Async is applied by a proxy, and a proxy is only
 * involved when the call arrives from OUTSIDE the object. If this method lived
 * on OrderEmailServiceImpl, that class calling its own method would bypass the
 * proxy entirely and send the mail synchronously — the classic self-invocation
 * trap, and a silent one: everything still works, just slowly.
 */
@Component
public class EmailDispatcher {

    private static final Logger log = LoggerFactory.getLogger(EmailDispatcher.class);

    private final JavaMailSender mailSender;
    private final InvoiceRenderer renderer;
    private final EmailProperties properties;

    public EmailDispatcher(JavaMailSender mailSender,
                           InvoiceRenderer renderer,
                           EmailProperties properties) {
        this.mailSender = mailSender;
        this.renderer = renderer;
        this.properties = properties;
    }

    @Async(AsyncConfig.EMAIL_EXECUTOR)
    public void sendOrderPlaced(OrderEmailRequest request, String requestId) {

        String orderNo = request.order().orderNo();
        String masked = request.customer().maskedEmail();

        String html;
        String text;
        String subject;
        try {
            subject = renderer.buildSubject(request.order());
            html = renderer.renderHtml(request);
            text = renderer.renderText(request);
        } catch (RuntimeException ex) {
            // A template fault is OUR bug, not the mail server's. Retrying it
            // would just fail three more times, so fail immediately and loudly.
            log.error("[{}] Could not render the invoice for orderNo={}", requestId, orderNo, ex);
            throw new EmailDeliveryException(orderNo, "Failed to render the order email", ex);
        }

        if (properties.isDryRun()) {
            log.info("""
                    [{}] DRY RUN - nothing was sent
                    To      : {}
                    Subject : {}
                    ---------------------------- text ----------------------------
                    {}
                    ---------------------------- html ----------------------------
                    {}
                    --------------------------------------------------------------""",
                    requestId, request.customer().email(), subject, text, html);
            return;
        }

        deliverWithRetries(request, requestId, subject, text, html, orderNo, masked);
    }

    /**
     * SMTP fails for two very different reasons: the mail server is briefly
     * busy or unreachable (worth another go in a moment), or the message is
     * simply unacceptable (bad address, blocked sender — retrying changes
     * nothing). We cannot cheaply tell them apart from a MailException, so we
     * retry a small fixed number of times and then stop. Retrying forever
     * would tie up a thread per doomed message.
     */
    private void deliverWithRetries(OrderEmailRequest request, String requestId, String subject,
                                    String text, String html, String orderNo, String masked) {

        int attempts = Math.max(1, properties.getMaxAttempts());
        MailException lastFailure = null;

        for (int attempt = 1; attempt <= attempts; attempt++) {
            try {
                mailSender.send(buildMessage(request, subject, text, html));

                log.info("[{}] Sent '{}' email for orderNo={} to {} on attempt {}/{}",
                        requestId, request.eventTypeOrDefault(), orderNo, masked, attempt, attempts);
                return;

            } catch (MailException ex) {
                lastFailure = ex;
                log.warn("[{}] Attempt {}/{} failed for orderNo={}: {}",
                        requestId, attempt, attempts, orderNo, ex.getMessage());

                if (attempt < attempts && !pause(properties.getRetryDelayMs())) {
                    break;   // shutting down - stop retrying
                }
            }
        }

        log.error("[{}] GAVE UP after {} attempts - orderNo={} recipient={}. "
                        + "The order itself is unaffected; only the email was lost.",
                requestId, attempts, orderNo, masked, lastFailure);

        throw new EmailDeliveryException(orderNo,
                "SMTP refused the message for orderNo=" + orderNo + " after " + attempts + " attempts",
                lastFailure);
    }

    private MimeMessage buildMessage(OrderEmailRequest request, String subject, String text, String html) {
        MimeMessage message = mailSender.createMimeMessage();
        try {
            // multipart=true so the message carries BOTH bodies: clients that
            // render HTML show the invoice, the rest show the text version.
            MimeMessageHelper helper = new MimeMessageHelper(
                    message, true, StandardCharsets.UTF_8.name());

            helper.setTo(request.customer().email());
            helper.setSubject(subject);
            helper.setText(text, html);   // (plain, html) - order matters

            try {
                helper.setFrom(properties.getFrom(), properties.getFromName());
            } catch (UnsupportedEncodingException ex) {
                // Only the display name failed to encode; the address is fine.
                helper.setFrom(properties.getFrom());
            }

            if (properties.getSupportEmail() != null && !properties.getSupportEmail().isBlank()) {
                helper.setReplyTo(properties.getSupportEmail());
            }

            return message;

        } catch (MessagingException ex) {
            throw new EmailDeliveryException(request.order().orderNo(),
                    "Could not assemble the MIME message", ex);
        }
    }

    /** @return false if the wait was interrupted, i.e. the service is stopping. */
    private boolean pause(long millis) {
        if (millis <= 0) return true;
        try {
            Thread.sleep(millis);
            return true;
        } catch (InterruptedException ex) {
            // Restore the flag rather than swallowing the interrupt, so the
            // pool can actually finish shutting down.
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
