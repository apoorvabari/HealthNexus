package org.proj.service.communication;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.proj.config.TwilioConfig;
import org.proj.service.SmsService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class TwilioMessagingServiceImpl implements TwilioMessagingService {

    private final SmsService smsService;
    private final TwilioCommunicationService twilioCommunicationService;
    private final VoiceService voiceService;
    private final TwilioConfig twilioConfig;

    @Override
    public boolean sendMessage(String toPhoneNumber, String messageBody, MessageChannel channel) {
        if (toPhoneNumber == null || toPhoneNumber.isBlank()) {
            log.warn("Cannot dispatch omnichannel message to null/blank phone number.");
            return false;
        }

        MessageChannel targetChannel = channel != null ? channel : MessageChannel.AUTO;

        switch (targetChannel) {
            case WHATSAPP:
                return twilioCommunicationService.sendWhatsApp(toPhoneNumber, messageBody);

            case VOICE:
                return voiceService.makeVoiceCall(toPhoneNumber, messageBody);

            case SMS:
                return smsService.sendSms(toPhoneNumber, messageBody);

            case AUTO:
            default:
                // If WhatsApp sender is configured, try WhatsApp first; if it returns false or is not configured, fallback to SMS
                if (twilioConfig.getWhatsappNumber() != null && !twilioConfig.getWhatsappNumber().isBlank()) {
                    boolean whatsAppSent = twilioCommunicationService.sendWhatsApp(toPhoneNumber, messageBody);
                    if (whatsAppSent) {
                        return true;
                    }
                }
                return smsService.sendSms(toPhoneNumber, messageBody);
        }
    }

    @Async("taskExecutor")
    @Override
    public CompletableFuture<Boolean> sendMessageAsync(String toPhoneNumber, String messageBody, MessageChannel channel) {
        boolean result = sendMessage(toPhoneNumber, messageBody, channel);
        return CompletableFuture.completedFuture(result);
    }
}
