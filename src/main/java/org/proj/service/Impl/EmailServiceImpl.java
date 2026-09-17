package org.proj.service.impl;

import lombok.RequiredArgsConstructor;
import org.proj.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private static final Logger logger =
            LoggerFactory.getLogger(EmailServiceImpl.class);

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.password-reset.frontend-url}")
    private String passwordResetFrontendUrl;

    @Value("${app.frontend.base-url}")
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

        try {

            mailSender.send(message);

            logger.info(
                    "Password reset email sent successfully");

        } catch (Exception e) {

            logger.error(
                    "Failed to send password reset email",
                    e);

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

        try {

            mailSender.send(message);

            logger.info(
                    "Email verification email sent successfully");

        } catch (Exception e) {

            logger.error(
                    "Failed to send verification email",
                    e);

            throw e;
        }
    }
}