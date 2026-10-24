package org.proj.service.communication;

import java.util.concurrent.CompletableFuture;

public interface VoiceService {

    /**
     * Synchronously initiate an automated outbound voice call delivering a text-to-speech message via TwiML.
     * @param toPhoneNumber Recipient phone number in E.164 format (e.g. +919876543210 or +14155552671)
     * @param textToSay Message spoken to the recipient when they answer
     * @return true if call queued/initiated, false otherwise
     */
    boolean makeVoiceCall(String toPhoneNumber, String textToSay);

    /**
     * Asynchronously initiate an automated outbound voice call delivering text-to-speech via TwiML.
     */
    CompletableFuture<Boolean> makeVoiceCallAsync(String toPhoneNumber, String textToSay);

    /**
     * Generate dynamic TwiML XML string with natural speech synthesis.
     * @param message Text to speak
     * @return Valid TwiML XML string
     */
    String generateTwiML(String message);
}
