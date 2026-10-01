package com.tailoredplatform.ecommerce.notifications.service;

/**
 * Kept behind an interface so the concrete sender (SMTP via Spring Mail
 * today; SES/SendGrid/etc. later) is a config change, not a call-site
 * change — same pattern as PaymentGateway in the payments module.
 */
public interface EmailService {
    void sendVerificationEmail(String toEmail, String fullName, String verificationToken);
    void sendPasswordResetEmail(String toEmail, String fullName, String resetToken);
    void sendOrderConfirmationEmail(String toEmail, String fullName, String orderNumber);
}
