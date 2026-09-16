package org.proj.controller;

import jakarta.validation.Valid;
import org.proj.dto.ConsultationRequest;
import org.proj.dto.ConsultationResponse;
import org.proj.service.ConsultationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/consultations")
public class ConsultationController {

    @Autowired
    private ConsultationService consultationService;

    @PostMapping("/start")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    public ResponseEntity<?> startConsultation(@Valid @RequestBody ConsultationRequest request) {
        try {
            ConsultationResponse response = consultationService.startConsultation(request);
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return buildErrorResponse(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (AccessDeniedException e) {
            return buildErrorResponse(HttpStatus.FORBIDDEN, e.getMessage());
        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An error occurred while starting consultation");
        }
    }

    @PutMapping("/{id}/visit")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    public ResponseEntity<?> saveVisit(
            @PathVariable UUID id,
            @RequestBody(required = false) Map<String, String> body) {

        try {

            String remarks = body != null
                    ? body.get("remarks")
                    : null;

            ConsultationResponse response =
                    consultationService.saveVisit(id, remarks);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {

            return buildErrorResponse(
                    HttpStatus.BAD_REQUEST,
                    e.getMessage()
            );

        } catch (AccessDeniedException e) {

            return buildErrorResponse(
                    HttpStatus.FORBIDDEN,
                    e.getMessage()
            );

        } catch (Exception e) {

            return buildErrorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "An error occurred while saving visit"
            );
        }
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    public ResponseEntity<?> completeConsultation(
            @PathVariable UUID id,
            @RequestBody(required = false) Map<String, String> body) {
        try {
            String remarks = body != null ? body.get("remarks") : null;
            ConsultationResponse response = consultationService.completeConsultation(id, remarks);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return buildErrorResponse(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (AccessDeniedException e) {
            return buildErrorResponse(HttpStatus.FORBIDDEN, e.getMessage());
        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An error occurred while completing consultation");
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'PATIENT')")
    public ResponseEntity<?> getConsultationById(@PathVariable UUID id) {
        try {
            return ResponseEntity.ok(consultationService.getConsultationById(id));
        } catch (IllegalArgumentException e) {
            return buildErrorResponse(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (AccessDeniedException e) {
            return buildErrorResponse(HttpStatus.FORBIDDEN, e.getMessage());
        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An error occurred while fetching consultation");
        }
    }

    @GetMapping("/appointment/{appointmentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'PATIENT')")
    public ResponseEntity<?> getConsultationByAppointmentId(@PathVariable UUID appointmentId) {
        try {
            return ResponseEntity.ok(consultationService.getConsultationByAppointmentId(appointmentId));
        } catch (IllegalArgumentException e) {
            return buildErrorResponse(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (AccessDeniedException e) {
            return buildErrorResponse(HttpStatus.FORBIDDEN, e.getMessage());
        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An error occurred while fetching consultation for appointment");
        }
    }

    private ResponseEntity<Map<String, String>> buildErrorResponse(HttpStatus status, String message) {
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("message", message);
        return new ResponseEntity<>(errorResponse, status);
    }
}
