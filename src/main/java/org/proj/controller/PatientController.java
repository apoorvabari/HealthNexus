package org.proj.controller;

import jakarta.validation.Valid;
import org.proj.dto.MedicalHistoryResponse;
import org.proj.dto.PatientRequest;
import org.proj.dto.PatientResponse;
import org.proj.service.MedicalHistoryService;
import org.proj.service.PatientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/patients")
public class PatientController {

    @Autowired
    private PatientService patientService;

    @Autowired
    private MedicalHistoryService medicalHistoryService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST', 'PATIENT')")
    public ResponseEntity<PatientResponse> createPatient(
            @Valid @RequestBody PatientRequest request) {

        PatientResponse response = patientService.createPatient(request);

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<PatientResponse> getMyProfile() {
        return ResponseEntity.ok(patientService.getMyProfile());
    }

    @GetMapping("/{patientId}/medical-history")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'PATIENT')")
    public ResponseEntity<?> getPatientMedicalHistory(
            @PathVariable UUID patientId) {

        try {

            List<MedicalHistoryResponse> history =
                    medicalHistoryService.getPatientMedicalHistory(patientId);

            return ResponseEntity.ok(history);

        } catch (AccessDeniedException e) {

            return buildErrorResponse(
                    HttpStatus.FORBIDDEN,
                    e.getMessage()
            );

        } catch (IllegalArgumentException e) {

            return buildErrorResponse(
                    HttpStatus.NOT_FOUND,
                    e.getMessage()
            );

        } catch (Exception e) {

            e.printStackTrace();

            return buildErrorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "An error occurred while fetching medical history"
            );
        }
    }

    @GetMapping("/{patientId}/medical-history/pdf")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'PATIENT')")
    public ResponseEntity<?> downloadMedicalHistoryPdf(
            @PathVariable UUID patientId) {

        try {

            byte[] pdf =
                    medicalHistoryService.generateMedicalHistoryPdf(patientId);

            return ResponseEntity.ok()
                    .header(
                            HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=healthnexus-medical-history-" + patientId + ".pdf"
                    )
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);

        } catch (AccessDeniedException e) {

            return buildErrorResponse(
                    HttpStatus.FORBIDDEN,
                    e.getMessage()
            );

        } catch (IllegalArgumentException e) {

            return buildErrorResponse(
                    HttpStatus.NOT_FOUND,
                    e.getMessage()
            );

        } catch (Exception e) {

            e.printStackTrace();

            return buildErrorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "An error occurred while generating medical history PDF"
            );
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'RECEPTIONIST', 'PATIENT')")
    public ResponseEntity<PatientResponse> getPatientById(
            @PathVariable UUID id) {

        return ResponseEntity.ok(patientService.getPatientById(id));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'RECEPTIONIST', 'PATIENT')")
    public ResponseEntity<?> getAllPatients() {

        try {

            return ResponseEntity.ok(patientService.getAllPatients());

        } catch (AccessDeniedException e) {

            return buildErrorResponse(
                    HttpStatus.FORBIDDEN,
                    e.getMessage()
            );

        } catch (Exception e) {

            e.printStackTrace();

            return buildErrorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "An error occurred while fetching patients"
            );
        }
    }

    @GetMapping("/by-account/{accountId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'RECEPTIONIST', 'PATIENT')")
    public ResponseEntity<PatientResponse> getPatientByAccountId(
            @PathVariable UUID accountId) {

        return ResponseEntity.ok(
                patientService.getPatientByAccountId(accountId)
        );
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST', 'PATIENT')")
    public ResponseEntity<PatientResponse> updatePatient(
            @PathVariable UUID id,
            @Valid @RequestBody PatientRequest request) {

        return ResponseEntity.ok(
                patientService.updatePatient(id, request)
        );
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST')")
    public ResponseEntity<String> deletePatient(
            @PathVariable UUID id) {

        patientService.deletePatient(id);

        return ResponseEntity.ok(
                "Patient profile deactivated successfully"
        );
    }

    private ResponseEntity<Map<String, String>> buildErrorResponse(
            HttpStatus status,
            String message) {

        Map<String, String> response = new HashMap<>();
        response.put("message", message);

        return new ResponseEntity<>(response, status);
    }
}
