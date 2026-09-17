package org.proj.service.impl;

import lombok.RequiredArgsConstructor;
import org.proj.service.EmailService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.password-reset.frontend-url}")
    private String passwordResetFrontendUrl;

    @Value("${app.frontend.base-url:http://localhost:8081}")
    private String frontendBaseUrl;

    @Override
    public void sendPasswordResetEmail(
            String email,
            String token) {

        String resetLink =
                passwordResetFrontendUrl
                        + "?token="
                        + token
                        + "&email="
                        + email;

        SimpleMailMessage message =
                new SimpleMailMessage();

        message.setFrom(fromEmail);
        message.setTo(email);
        message.setSubject("HealthNexus - Password Reset");

        message.setText(
                "Hello,\n\n"
                        + "We received a request to reset your HealthNexus password.\n\n"
                        + "Use the following link to reset your password:\n\n"
                        + resetLink
                        + "\n\n"
                        + "This link will expire in 30 minutes.\n\n"
                        + "If you did not request a password reset, you can safely ignore this email.\n\n"
                        + "HealthNexus"
        );

        System.out.println("========================================");
        System.out.println("PASSWORD RESET EMAIL");
        System.out.println("FROM: " + fromEmail);
        System.out.println("TO: " + email);
        System.out.println("========================================");

        try {

            mailSender.send(message);

            System.out.println("EMAIL SENT SUCCESSFULLY");
            System.out.println("========================================");

        } catch (Exception e) {

            System.err.println("EMAIL SENDING FAILED");
            System.err.println("ERROR: " + e.getClass().getName());
            System.err.println("MESSAGE: " + e.getMessage());

            throw e;
        }
    }

    @Override
    public void sendVerificationEmail(
            String email,
            String token) {

        String verifyLink =
                frontendBaseUrl
                        + "/verify-email?token="
                        + token
                        + "&email="
                        + email;

        SimpleMailMessage message =
                new SimpleMailMessage();

        message.setFrom(fromEmail);
        message.setTo(email);
        message.setSubject("HealthNexus - Email Verification");

        message.setText(
                "Hello,\n\n"
                        + "Welcome to HealthNexus! Please verify your email address by clicking the link below:\n\n"
                        + verifyLink
                        + "\n\n"
                        + "This verification link will expire in 24 hours.\n\n"
                        + "If you did not create a HealthNexus account, you can safely ignore this email.\n\n"
                        + "HealthNexus"
        );

        System.out.println("========================================");
        System.out.println("EMAIL VERIFICATION");
        System.out.println("FROM: " + fromEmail);
        System.out.println("TO: " + email);
        System.out.println("========================================");

        try {

            mailSender.send(message);

            System.out.println("VERIFICATION EMAIL SENT SUCCESSFULLY");
            System.out.println("========================================");

        } catch (Exception e) {

            System.err.println("VERIFICATION EMAIL SENDING FAILED");
            System.err.println("ERROR: " + e.getClass().getName());
            System.err.println("MESSAGE: " + e.getMessage());

            throw e;
        }
    }
}
