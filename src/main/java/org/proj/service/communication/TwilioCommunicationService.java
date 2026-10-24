package org.proj.service.communication;

import org.proj.service.SmsService;
import java.util.concurrent.CompletableFuture;

public interface TwilioCommunicationService extends SmsService {

    /**
     * Synchronously dispatch a WhatsApp message via Twilio Messaging API.
     * @param toPhoneNumber Recipient phone number in E.164 format
     * @param messageBody Text body of the WhatsApp message
     * @return true if accepted by Twilio, false otherwise
     */
    boolean sendWhatsApp(String toPhoneNumber, String messageBody);

    /**
     * Asynchronously dispatch a WhatsApp message.
     */
    CompletableFuture<Boolean> sendWhatsAppAsync(String toPhoneNumber, String messageBody);

    /**
     * Synchronously initiate an automated outbound voice call delivering a text-to-speech message via TwiML.
     * @param toPhoneNumber Recipient phone number in E.164 format
     * @param textToSay Message spoken to the recipient when they answer
     * @return true if call queued/initiated, false otherwise
     */
    boolean makeVoiceCall(String toPhoneNumber, String textToSay);

    /**
     * Asynchronously initiate an automated outbound voice call delivering text-to-speech via TwiML.
     */
    CompletableFuture<Boolean> makeVoiceCallAsync(String toPhoneNumber, String textToSay);
}
