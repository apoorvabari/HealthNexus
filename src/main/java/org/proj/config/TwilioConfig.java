package org.proj.config;

import com.twilio.Twilio;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class TwilioConfig {

    @Value("${twilio.account-sid:}")
    private String accountSid;

    @Value("${twilio.auth-token:}")
    private String authToken;

    @Getter
    @Value("${twilio.phone-number:}")
    private String phoneNumber;

    @Getter
    @Value("${twilio.whatsapp-number:}")
    private String whatsappNumber;

    @Getter
    @Value("${twilio.messaging-service-sid:}")
    private String messagingServiceSid;

    @Getter
    @Value("${twilio.enabled:false}")
    private boolean enabled;

    @PostConstruct
    public void initTwilio() {
        if (!enabled) {
            log.info("Twilio Communication Services are DISABLED (twilio.enabled=false). Calls, SMS, and WhatsApp will be safely bypassed.");
            return;
        }

        if (accountSid == null || accountSid.isBlank() || authToken == null || authToken.isBlank()) {
            log.warn("Twilio is marked as enabled, but TWILIO_ACCOUNT_SID or TWILIO_AUTH_TOKEN is missing. Disabling communications.");
            this.enabled = false;
            return;
        }

        try {
            Twilio.init(accountSid.trim(), authToken.trim());
            log.info("Twilio client successfully initialized for Sender Number: {} | WhatsApp: {}", 
                    phoneNumber, (whatsappNumber != null && !whatsappNumber.isBlank() ? whatsappNumber : "Not Configured"));
        } catch (Exception e) {
            log.error("Failed to initialize Twilio client: {}. Disabling Twilio communications.", e.getMessage(), e);
            this.enabled = false;
        }
    }
}
