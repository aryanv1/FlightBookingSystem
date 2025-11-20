package com.project.flightbooking.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    // JavaMailSender is Spring Boot’s email-sending interface.
    // Internally, it connects to the configured SMTP server (like Gmail SMTP, SendGrid SMTP, etc.)
    private final JavaMailSender mailSender;

    @Value("${app.mail.from:no-reply@yourapp.com}")
    // If no values is there then will use this default value
    private String from;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendPlainEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(from);
            msg.setTo(to);
            msg.setSubject(subject);
            msg.setText(body);
            // SMTP connection is opened
            // Authentication happens
            // Message is delivered to mail server
            // Then mail server attempts to deliver to recipient
            mailSender.send(msg);
        } catch (MailException e) {
            // Log but do not rethrow (email failure should not break payment/refund).
            // Because email is a secondary feature.
            // We don’t want to cancel the payment just because email sending failed.
            System.err.println("Failed to send email to " + to + " : " + e.getMessage());
        }
    }
}