package org.proj.controller;

import jakarta.validation.Valid;
import org.proj.dto.ConsentRequestDto;
import org.proj.dto.ConsentResponse;
import org.proj.entity.PatientConsentEntity.ConsentType;
import org.proj.exception.BadRequestException;
import org.proj.exception.ResourceNotFoundException;
import org.proj.service.PatientConsentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import org.proj.entity.PatientConsentEntity;
import org.proj.dto.ConsentRequest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping({"/api/consents", "/consents"})
public class PatientConsentController {

    @Autowired
    private PatientConsentService patientConsentService;

    @PostMapping(path = {"", "/grant"})
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<?> grantOrUpdateConsent(@Valid @RequestBody ConsentRequestDto request) {
        try {
            ConsentResponse response = patientConsentService.grantDoctorConsent(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException | BadRequestException e) {
            return buildErrorResponse(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (ResourceNotFoundException e) {
            return buildErrorResponse(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (AccessDeniedException e) {
            return buildErrorResponse(HttpStatus.FORBIDDEN, e.getMessage());
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : "Unable to update consent";
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, msg);
        }
    }

    @PostMapping(path = {"/standard/toggle", "/standard", "/toggle"})
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<?> toggleStandardConsent(@Valid @RequestBody ConsentRequest request) {
        try {
            ConsentResponse response = patientConsentService.toggleStandardConsent(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException | BadRequestException e) {
            return buildErrorResponse(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (ResourceNotFoundException e) {
            return buildErrorResponse(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (AccessDeniedException e) {
            return buildErrorResponse(HttpStatus.FORBIDDEN, e.getMessage());
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : "Unable to toggle standard consent";
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, msg);
        }
    }

    @PutMapping("/{type}/status")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<?> updateConsentStatusByType(
            @PathVariable String type,
            @RequestBody Map<String, String> body) {
        try {
            ConsentType consentType;
            String upper = type.trim().toUpperCase();
            if (upper.equals("MEDICAL_RECORDS") || upper.equals("ALL")) {
                consentType = ConsentType.MEDICAL_RECORD_SHARING;
            } else {
                consentType = ConsentType.valueOf(upper);
            }
            String statusStr = body != null && body.containsKey("status") ? body.get("status").trim().toUpperCase() : "GRANTED";
            PatientConsentEntity.ConsentStatus status;
            try {
                status = PatientConsentEntity.ConsentStatus.valueOf(statusStr);
            } catch (Exception ignored) {
                status = PatientConsentEntity.ConsentStatus.GRANTED;
            }
            org.proj.dto.ConsentRequest request = org.proj.dto.ConsentRequest.builder()
                    .consentType(consentType)
                    .status(status)
                    .version("v1.0")
                    .build();
            ConsentResponse response = patientConsentService.toggleStandardConsent(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException | BadRequestException e) {
            return buildErrorResponse(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (ResourceNotFoundException e) {
            return buildErrorResponse(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (AccessDeniedException e) {
            return buildErrorResponse(HttpStatus.FORBIDDEN, e.getMessage());
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : "Unable to update consent status";
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, msg);
        }
    }

    @PutMapping("/revoke/{doctorId}")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<?> revokeDoctorConsent(@PathVariable String doctorId) {
        try {
            ConsentResponse response = patientConsentService.revokeDoctorConsent(doctorId);
            return ResponseEntity.ok(Map.of("message", "Consent revoked successfully", "consent", response));
        } catch (IllegalArgumentException | BadRequestException e) {
            return buildErrorResponse(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (ResourceNotFoundException e) {
            return buildErrorResponse(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (AccessDeniedException e) {
            return buildErrorResponse(HttpStatus.FORBIDDEN, e.getMessage());
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : "Unable to revoke consent";
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, msg);
        }
    }

    @GetMapping(path = {"", "/my-consents"})
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<?> getMyConsents() {
        try {
            List<ConsentResponse> response = patientConsentService.getMyConsents();
            return ResponseEntity.ok(response);
        } catch (AccessDeniedException e) {
            return buildErrorResponse(HttpStatus.FORBIDDEN, e.getMessage());
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : "Unable to fetch consents";
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, msg);
        }
    }

    @GetMapping("/{type}")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<?> getConsentByType(@PathVariable String type) {
        try {
            ConsentType consentType;
            String upper = type.trim().toUpperCase();
            if (upper.equals("MEDICAL_RECORDS") || upper.equals("ALL")) {
                consentType = ConsentType.MEDICAL_RECORD_SHARING;
            } else {
                consentType = ConsentType.valueOf(upper);
            }
            ConsentResponse response = patientConsentService.getConsentByType(consentType);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return buildErrorResponse(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (AccessDeniedException e) {
            return buildErrorResponse(HttpStatus.FORBIDDEN, e.getMessage());
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : "Unable to fetch consent";
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, msg);
        }
    }

    @PatchMapping("/{type}/revoke")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<?> revokeConsent(
            @PathVariable String type,
            @RequestParam(required = false) UUID doctorId) {
        try {
            ConsentType consentType;
            String upper = type.trim().toUpperCase();
            if (upper.equals("MEDICAL_RECORDS") || upper.equals("ALL")) {
                consentType = ConsentType.MEDICAL_RECORD_SHARING;
            } else {
                consentType = ConsentType.valueOf(upper);
            }

            ConsentResponse response = (doctorId != null)
                    ? patientConsentService.revokeConsentForDoctor(consentType, doctorId)
                    : patientConsentService.revokeConsent(consentType);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return buildErrorResponse(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (AccessDeniedException e) {
            return buildErrorResponse(HttpStatus.FORBIDDEN, e.getMessage());
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : "Unable to revoke consent";
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, msg);
        }
    }

    private ResponseEntity<Map<String, String>> buildErrorResponse(HttpStatus status, String message) {
        Map<String, String> response = new HashMap<>();
        response.put("message", message);
        return new ResponseEntity<>(response, status);
    }
}
