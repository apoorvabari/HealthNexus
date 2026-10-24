package org.proj.service.communication;

import com.twilio.exception.ApiException;
import com.twilio.rest.api.v2010.account.Call;
import com.twilio.type.PhoneNumber;
import com.twilio.type.Twiml;
import com.twilio.twiml.VoiceResponse;
import com.twilio.twiml.voice.Say;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.proj.config.TwilioConfig;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class TwilioVoiceServiceImpl implements VoiceService {

    private final TwilioConfig twilioConfig;

    private static final Pattern E164_PATTERN = Pattern.compile("^\\+[1-9]\\d{6,14}$");

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
            String twimlXml = generateTwiML(textToSay);

            Call call = Call.creator(
                    new PhoneNumber(sanitizedPhone),
                    new PhoneNumber(twilioConfig.getPhoneNumber().trim()),
                    new Twiml(twimlXml)
            )
            .setStatusCallback(URI.create(twilioConfig.getCallbackBaseUrl() + "/api/public/twilio/status-callback"))
            .create();

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

    @Override
    public String generateTwiML(String message) {
        String safeText = (message != null && !message.isBlank()) ? message.trim() : "Hello. This is an automated notification from HealthNexus.";
        try {
            VoiceResponse voiceResponse = new VoiceResponse.Builder()
                    .say(new Say.Builder(safeText)
                            .voice(Say.Voice.POLLY_ADITI)
                            .language(Say.Language.EN_IN)
                            .build())
                    .build();
            return voiceResponse.toXml();
        } catch (Exception e) {
            // Fallback to standard Alice voice if Polly is unavailable
            VoiceResponse fallback = new VoiceResponse.Builder()
                    .say(new Say.Builder(safeText)
                            .voice(Say.Voice.ALICE)
                            .language(Say.Language.EN_US)
                            .build())
                    .build();
            return fallback.toXml();
        }
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
