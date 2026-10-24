package org.proj.service.communication;

import com.twilio.exception.ApiException;
import com.twilio.rest.api.v2010.account.Call;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import com.twilio.type.Twiml;
import com.twilio.twiml.VoiceResponse;
import com.twilio.twiml.voice.Say;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.proj.config.TwilioConfig;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class TwilioCommunicationServiceImpl implements TwilioCommunicationService {

    private final TwilioConfig twilioConfig;

    // Strict E.164 pattern: '+' followed by 7 to 15 digits
    private static final Pattern E164_PATTERN = Pattern.compile("^\\+[1-9]\\d{6,14}$");

    @Override
    public boolean sendSms(String toPhoneNumber, String messageBody) {
        if (!twilioConfig.isEnabled()) {
            log.debug("Twilio SMS disabled. Skipping dispatch to {}", maskPhone(toPhoneNumber));
            return false;
        }

        String sanitizedPhone = sanitizePhoneNumber(toPhoneNumber);
        if (!isValidE164(sanitizedPhone)) {
            log.warn("Invalid recipient phone number '{}' for SMS. Must match E.164 standard (e.g., +919876543210).", toPhoneNumber);
            return false;
        }

        if (messageBody == null || messageBody.isBlank()) {
            log.warn("Cannot send empty SMS to {}", maskPhone(sanitizedPhone));
            return false;
        }

        try {
            Message message;
            if (twilioConfig.getMessagingServiceSid() != null && !twilioConfig.getMessagingServiceSid().isBlank()) {
                message = Message.creator(
                        new PhoneNumber(sanitizedPhone),
                        twilioConfig.getMessagingServiceSid().trim(),
                        messageBody
                ).create();
            } else {
                message = Message.creator(
                        new PhoneNumber(sanitizedPhone),
                        new PhoneNumber(twilioConfig.getPhoneNumber().trim()),
                        messageBody
                ).create();
            }

            log.info("Twilio SMS successfully queued/sent to {}. SID: {}, Status: {}", 
                    maskPhone(sanitizedPhone), message.getSid(), message.getStatus());
            return true;
        } catch (ApiException e) {
            log.error("Twilio API Error sending SMS to {}: Code={}, Message={}", 
                    maskPhone(sanitizedPhone), e.getCode(), e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("Unexpected error sending SMS to {}: {}", 
                    maskPhone(sanitizedPhone), e.getMessage(), e);
            return false;
        }
    }

    @Async("taskExecutor")
    @Override
    public CompletableFuture<Boolean> sendSmsAsync(String toPhoneNumber, String messageBody) {
        boolean result = sendSms(toPhoneNumber, messageBody);
        return CompletableFuture.completedFuture(result);
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
        if (!twilioConfig.isEnabled()) {
            log.debug("Twilio Voice Calls disabled. Skipping call to {}", maskPhone(toPhoneNumber));
            return false;
        }

        String sanitizedPhone = sanitizePhoneNumber(toPhoneNumber);
        if (!isValidE164(sanitizedPhone)) {
            log.warn("Invalid recipient phone number '{}' for Voice Call. Must match E.164 standard.", toPhoneNumber);
            return false;
        }

        if (textToSay == null || textToSay.isBlank()) {
            log.warn("Cannot initiate voice call with empty message to {}", maskPhone(sanitizedPhone));
            return false;
        }

        try {
            VoiceResponse voiceResponse = new VoiceResponse.Builder()
                    .say(new Say.Builder(textToSay.trim())
                            .voice(Say.Voice.ALICE)
                            .language(Say.Language.EN_US)
                            .build())
                    .build();

            Call call = Call.creator(
                    new PhoneNumber(sanitizedPhone),
                    new PhoneNumber(twilioConfig.getPhoneNumber().trim()),
                    new Twiml(voiceResponse.toXml())
            ).create();

            log.info("Twilio Voice Call initiated to {}. Call SID: {}, Status: {}", 
                    maskPhone(sanitizedPhone), call.getSid(), call.getStatus());
            return true;
        } catch (ApiException e) {
            log.error("Twilio Voice Call API Error calling {}: Code={}, Message={}", 
                    maskPhone(sanitizedPhone), e.getCode(), e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("Unexpected error placing voice call to {}: {}", 
                    maskPhone(sanitizedPhone), e.getMessage(), e);
            return false;
        }
    }

    @Async("taskExecutor")
    @Override
    public CompletableFuture<Boolean> makeVoiceCallAsync(String toPhoneNumber, String textToSay) {
        boolean result = makeVoiceCall(toPhoneNumber, textToSay);
        return CompletableFuture.completedFuture(result);
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
