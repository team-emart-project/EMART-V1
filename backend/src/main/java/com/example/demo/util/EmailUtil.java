package com.example.demo.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Outbound email.
 *
 * NOTE: this currently LOGS instead of sending. The BRD puts email behind a
 * separate Notification/PDF microservice which is not built yet, and wiring
 * real SMTP into a college project means committing credentials.
 *
 * Everything the user would have received appears in the console, so you can
 * copy a reset token straight out of the log and test the reset endpoint.
 * When the Notification service exists, replace the bodies of these methods
 * with a call to NotificationServiceClient — no caller has to change.
 */
@Component
public class EmailUtil {
    private static final Logger log = LoggerFactory.getLogger(EmailUtil.class);


    public void sendMembershipNumber(String toEmail, String firstName, String membershipNo) {
        log.info("""

                ===================== EMAIL (not actually sent) =====================
                To      : {}
                Subject : Welcome to e-MART
                Body    : Hi {}, thanks for registering.
                          Your membership number is {}.
                =====================================================================
                """, toEmail, firstName, membershipNo);
    }

    public void sendPasswordResetToken(String toEmail, String resetToken) {
        log.info("""

                ===================== EMAIL (not actually sent) =====================
                To      : {}
                Subject : Reset your e-MART password
                Body    : Use this token within 30 minutes to reset your password:
                          {}
                          (POST it to /api/auth/reset-password)
                =====================================================================
                """, toEmail, resetToken);
    }

    public void sendCardApplicationReceived(String toEmail, String firstName) {
        log.info("""

                ===================== EMAIL (not actually sent) =====================
                To      : {}
                Subject : e-MART Card application received
                Body    : Hi {}, we have received your e-MART card application.
                          It is currently PENDING review.
                =====================================================================
                """, toEmail, firstName);
    }
}
