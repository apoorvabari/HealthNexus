package org.proj.service.communication;

import java.util.concurrent.CompletableFuture;

public interface TwilioMessagingService {

    enum MessageChannel {
        SMS,
        WHATSAPP,
        VOICE,
        AUTO
    }

    /**
     * Dispatch an omnichannel message (SMS or WhatsApp) based on specified channel.
     * @param toPhoneNumber Destination phone number in E.164 format
     * @param messageBody Text body of the message
     * @param channel Preferred channel (SMS, WHATSAPP, VOICE, or AUTO)
     * @return true if successfully dispatched, false otherwise
     */
    boolean sendMessage(String toPhoneNumber, String messageBody, MessageChannel channel);

    /**
     * Asynchronously dispatch an omnichannel message.
     */
    CompletableFuture<Boolean> sendMessageAsync(String toPhoneNumber, String messageBody, MessageChannel channel);
}
