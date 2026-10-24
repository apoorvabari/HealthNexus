package org.proj.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.proj.service.communication.VoiceService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/public/twilio")
@RequiredArgsConstructor
public class TwilioCallbackController {

    private final VoiceService voiceService;

    /**
     * Dynamically serve TwiML instructions when Twilio executes an outbound voice call.
     */
    @PostMapping(value = "/voice-twiml", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> getVoiceTwiml(
            @RequestParam(value = "message", required = false) String message,
            @RequestParam Map<String, String> allParams
    ) {
        log.info("Serving dynamic TwiML for Twilio voice callback. Params: {}", allParams.keySet());
        String textToSpeak = (message != null && !message.isBlank())
                ? message
                : "Hello. This is an automated notification from HealthNexus.";
        String twimlXml = voiceService.generateTwiML(textToSpeak);
        return ResponseEntity.ok(twimlXml);
    }

    /**
     * Track delivery receipts and call completion status from Twilio.
     */
    @PostMapping("/status-callback")
    public ResponseEntity<Void> handleStatusCallback(@RequestParam Map<String, String> payload) {
        String messageSid = payload.get("MessageSid");
        String callSid = payload.get("CallSid");
        String messageStatus = payload.get("MessageStatus");
        String callStatus = payload.get("CallStatus");
        String to = payload.get("To");

        if (callSid != null) {
            log.info("Twilio Voice Call Status Event: CallSid={}, To={}, CallStatus={}", callSid, to, callStatus);
        } else if (messageSid != null) {
            log.info("Twilio Message Status Event: MessageSid={}, To={}, MessageStatus={}", messageSid, to, messageStatus);
        } else {
            log.info("Twilio Status Event: {}", payload);
        }

        return ResponseEntity.ok().build();
    }
}
