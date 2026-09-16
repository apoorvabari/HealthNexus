package org.proj.service;

public interface EmailService {

    void sendPasswordResetEmail(
            String email,
            String token
    );

    void sendVerificationEmail(
            String email,
            String token
    );
}
