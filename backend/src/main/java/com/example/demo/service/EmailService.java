package com.example.demo.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Handles outgoing emails for verification / password reset.
 *
 * NOTE for local/Postman testing: if you haven't configured real SMTP
 * credentials in application.properties, sending will fail/throw.
 * The token is always logged to the console as well, so you can copy
 * it straight from the console log and use it in Postman without
 * needing a working mail server.
 */
@Service
@Slf4j
public class EmailService {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    public void sendVerificationEmail(String toEmail, String token) {
        log.info("=== EMAIL VERIFICATION TOKEN for {} ===  {}", toEmail, token);
        String body = "Welcome to eMART! Use this token to verify your email: " + token;
        trySend(toEmail, "Verify your eMART account", body);
    }

    public void sendPasswordResetEmail(String toEmail, String token) {
        log.info("=== PASSWORD RESET TOKEN for {} ===  {}", toEmail, token);
        String body = "Use this token to reset your eMART password: " + token
                + "\nThis token expires in 15 minutes.";
        trySend(toEmail, "Reset your eMART password", body);
    }

    private void trySend(String toEmail, String subject, String body) {
        if (mailSender == null) {
            log.warn("JavaMailSender not configured; skipping actual email send. Use the token logged above.");
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", toEmail, e.getMessage());
        }
    }
}
