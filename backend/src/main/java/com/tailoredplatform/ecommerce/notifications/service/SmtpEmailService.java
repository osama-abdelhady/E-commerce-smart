package com.tailoredplatform.ecommerce.notifications.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class SmtpEmailService implements EmailService {

    private final JavaMailSender mailSender;

    @Override
    public void sendVerificationEmail(String toEmail, String fullName, String verificationToken) {
        send(toEmail, "Verify your email",
                "Hi " + fullName + ",\n\nPlease verify your email using this code: " + verificationToken
                        + "\n\nIf you didn't create an account, you can ignore this message.");
    }

    @Override
    public void sendPasswordResetEmail(String toEmail, String fullName, String resetToken) {
        send(toEmail, "Reset your password",
                "Hi " + fullName + ",\n\nUse this code to reset your password: " + resetToken
                        + "\n\nThis code expires in 30 minutes. If you didn't request this, you can ignore this message.");
    }

    @Override
    public void sendOrderConfirmationEmail(String toEmail, String fullName, String orderNumber) {
        send(toEmail, "Order confirmed — " + orderNumber,
                "Hi " + fullName + ",\n\nYour order " + orderNumber + " has been confirmed. "
                        + "We'll email you again once it ships.");
    }

    private void send(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
        } catch (Exception ex) {
            // Email delivery failure must never fail the calling business
            // transaction (e.g. registration or order placement) — log and
            // move on. A production build would push this to a retry queue.
            log.error("Failed to send email to {}: {}", to, ex.getMessage());
        }
    }
}
