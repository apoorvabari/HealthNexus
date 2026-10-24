package org.proj.service;

import java.util.concurrent.CompletableFuture;

public interface SmsService {
    /**
     * Synchronously send an SMS alert to a recipient.
     * @param toPhoneNumber Recipient phone number in E.164 format (e.g. +919876543210 or +14155552671)
     * @param messageBody Text body of the SMS
     * @return true if accepted by Twilio, false otherwise
     */
    boolean sendSms(String toPhoneNumber, String messageBody);

    /**
     * Asynchronously send an SMS alert without blocking HTTP request threads.
     */
    CompletableFuture<Boolean> sendSmsAsync(String toPhoneNumber, String messageBody);
}
