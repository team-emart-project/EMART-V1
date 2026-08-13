package com.example.demo.service.implementation;

import com.example.demo.config.EmailProperties;
import com.example.demo.dto.request.OrderEmailRequest;
import com.example.demo.dto.response.EmailSendResponse;
import com.example.demo.service.interfaces.OrderEmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class OrderEmailServiceImpl implements OrderEmailService {

    private static final Logger log = LoggerFactory.getLogger(OrderEmailServiceImpl.class);

    private final EmailDispatcher dispatcher;
    private final EmailProperties properties;

    public OrderEmailServiceImpl(EmailDispatcher dispatcher, EmailProperties properties) {
        this.dispatcher = dispatcher;
        this.properties = properties;
    }

    @Override
    public EmailSendResponse acceptOrderPlaced(OrderEmailRequest request) {

        String requestId = UUID.randomUUID().toString().substring(0, 8);
        String orderNo = request.order().orderNo();
        String masked = request.customer().maskedEmail();

        log.info("[{}] Received '{}' from {} - orderNo={} items={} total={} recipient={}",
                requestId,
                request.eventTypeOrDefault(),
                request.sourceSystemOrUnknown(),
                orderNo,
                request.order().items().size(),
                request.order().totalAmount(),
                masked);

        if (!properties.isEnabled()) {
            // Still a 2xx. The caller placed a valid order and did nothing
            // wrong; sending is switched off at OUR end, and turning that into
            // an error would put a red line in the checkout log every time.
            log.info("[{}] emart.email.enabled=false - skipping orderNo={}", requestId, orderNo);
            return EmailSendResponse.skipped(requestId, orderNo, masked);
        }

        dispatcher.sendOrderPlaced(request, requestId);

        return EmailSendResponse.accepted(requestId, orderNo, masked);
    }
}
