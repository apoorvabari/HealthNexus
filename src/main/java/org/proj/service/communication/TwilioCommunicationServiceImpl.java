package org.proj.service.communication;

import com.twilio.exception.ApiException;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.proj.config.TwilioConfig;
import org.proj.service.SmsService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class TwilioCommunicationServiceImpl implements TwilioCommunicationService {

    private final TwilioConfig twilioConfig;
    private final SmsService smsService;
    private final VoiceService voiceService;

    private static final Pattern E164_PATTERN = Pattern.compile("^\\+[1-9]\\d{6,14}$");

    @Override
    public boolean sendSms(String toPhoneNumber, String messageBody) {
        return smsService.sendSms(toPhoneNumber, messageBody);
    }

    @Async("taskExecutor")
    @Override
    public CompletableFuture<Boolean> sendSmsAsync(String toPhoneNumber, String messageBody) {
        return smsService.sendSmsAsync(toPhoneNumber, messageBody);
    }

    @Override
    public boolean sendWhatsApp(String toPhoneNumber, String messageBody) {
        if (!twilioConfig.isEnabled()) {
            log.debug("Twilio WhatsApp disabled. Skipping dispatch to {}", maskPhone(toPhoneNumber));
            return false;
        }

        String sanitizedPhone = sanitizePhoneNumber(toPhoneNumber);
        if (!isValidE164(sanitizedPhone)) {
            log.warn("Invalid recipient phone number '{}' for WhatsApp. Must match E.164 standard.", toPhoneNumber);
            return false;
        }

        String fromWhatsapp = twilioConfig.getWhatsappNumber();
        if (fromWhatsapp == null || fromWhatsapp.isBlank()) {
            fromWhatsapp = twilioConfig.getPhoneNumber();
        }

        if (fromWhatsapp == null || fromWhatsapp.isBlank()) {
            log.warn("No sender phone or WhatsApp number configured for Twilio WhatsApp.");
            return false;
        }

        String toWhatsappFormatted = sanitizedPhone.startsWith("whatsapp:") ? sanitizedPhone : "whatsapp:" + sanitizedPhone;
        String fromWhatsappFormatted = fromWhatsapp.startsWith("whatsapp:") ? fromWhatsapp.trim() : "whatsapp:" + fromWhatsapp.trim();

        try {
            Message message = Message.creator(
                    new PhoneNumber(toWhatsappFormatted),
                    new PhoneNumber(fromWhatsappFormatted),
                    messageBody
            ).create();

            log.info("Twilio WhatsApp message queued/sent to {}. SID: {}, Status: {}", 
                    maskPhone(sanitizedPhone), message.getSid(), message.getStatus());
            return true;
        } catch (ApiException e) {
            log.error("Twilio WhatsApp API Error sending to {}: Code={}, Message={}", 
                    maskPhone(sanitizedPhone), e.getCode(), e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("Unexpected error sending WhatsApp message to {}: {}", 
                    maskPhone(sanitizedPhone), e.getMessage(), e);
            return false;
        }
    }

    @Async("taskExecutor")
    @Override
    public CompletableFuture<Boolean> sendWhatsAppAsync(String toPhoneNumber, String messageBody) {
        boolean result = sendWhatsApp(toPhoneNumber, messageBody);
        return CompletableFuture.completedFuture(result);
    }

    @Override
    public boolean makeVoiceCall(String toPhoneNumber, String textToSay) {
        return voiceService.makeVoiceCall(toPhoneNumber, textToSay);
    }

    @Async("taskExecutor")
    @Override
    public CompletableFuture<Boolean> makeVoiceCallAsync(String toPhoneNumber, String textToSay) {
        return voiceService.makeVoiceCallAsync(toPhoneNumber, textToSay);
    }

    private String sanitizePhoneNumber(String phone) {
        if (phone == null) return "";
        String cleaned = phone.trim().replaceAll("[\\s\\-\\(\\)]", "");
        if (cleaned.startsWith("whatsapp:")) {
            cleaned = cleaned.substring("whatsapp:".length());
        }
        return cleaned;
    }

    private boolean isValidE164(String phone) {
        return phone != null && E164_PATTERN.matcher(phone).matches();
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() <= 4) return "****";
        String clean = phone.replace("whatsapp:", "");
        if (clean.length() <= 4) return "****";
        return clean.substring(0, Math.min(clean.length() - 4, 3)) + "****" + clean.substring(clean.length() - 4);
    }
}
