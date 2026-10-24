package org.proj.service.communication;

import com.twilio.exception.ApiException;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.proj.config.TwilioConfig;
import org.proj.service.SmsService;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

@Slf4j
@Primary
@Service
@RequiredArgsConstructor
public class TwilioSmsServiceImpl implements SmsService {

    private final TwilioConfig twilioConfig;

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

    private String sanitizePhoneNumber(String phone) {
        if (phone == null) return "";
        return phone.trim().replaceAll("[\\s\\-\\(\\)]", "");
    }

    private boolean isValidE164(String phone) {
        return phone != null && E164_PATTERN.matcher(phone).matches();
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() <= 4) return "****";
        return phone.substring(0, Math.min(phone.length() - 4, 3)) + "****" + phone.substring(phone.length() - 4);
    }
}
