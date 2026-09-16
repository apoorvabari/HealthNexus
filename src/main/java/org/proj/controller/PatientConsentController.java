package org.proj.controller;

import jakarta.validation.Valid;
import org.proj.dto.ConsentRequest;
import org.proj.dto.ConsentResponse;
import org.proj.entity.PatientConsentEntity.ConsentType;
import org.proj.service.PatientConsentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/consents")
public class PatientConsentController {

    @Autowired
    private PatientConsentService patientConsentService;

    @PostMapping
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<?> updateConsent(@Valid @RequestBody ConsentRequest request) {
        try {
            ConsentResponse response = patientConsentService.updateConsent(request);
            return ResponseEntity.ok(response);
        } catch (AccessDeniedException e) {
            return buildErrorResponse(HttpStatus.FORBIDDEN, e.getMessage());
        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to update consent");
        }
    }

    @GetMapping
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<?> getMyConsents() {
        try {
            List<ConsentResponse> response = patientConsentService.getMyConsents();
            return ResponseEntity.ok(response);
        } catch (AccessDeniedException e) {
            return buildErrorResponse(HttpStatus.FORBIDDEN, e.getMessage());
        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to fetch consents");
        }
    }

    @GetMapping("/{type}")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<?> getConsentByType(@PathVariable ConsentType type) {
        try {
            ConsentResponse response = patientConsentService.getConsentByType(type);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return buildErrorResponse(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (AccessDeniedException e) {
            return buildErrorResponse(HttpStatus.FORBIDDEN, e.getMessage());
        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to fetch consent");
        }
    }

    @PatchMapping("/{type}/revoke")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<?> revokeConsent(@PathVariable ConsentType type) {
        try {
            ConsentResponse response = patientConsentService.revokeConsent(type);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return buildErrorResponse(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (AccessDeniedException e) {
            return buildErrorResponse(HttpStatus.FORBIDDEN, e.getMessage());
        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to revoke consent");
        }
    }

    private ResponseEntity<Map<String, String>> buildErrorResponse(HttpStatus status, String message) {
        Map<String, String> response = new HashMap<>();
        response.put("message", message);
        return new ResponseEntity<>(response, status);
    }
}
