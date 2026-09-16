package org.proj.controller;

import jakarta.validation.Valid;
import org.proj.dto.ReceptionistRequest;
import org.proj.dto.ReceptionistResponse;
import org.proj.service.ReceptionistService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/receptionists")

public class ReceptionistController {

    @Autowired
    private ReceptionistService receptionistService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReceptionistResponse> createReceptionist(
            @Valid @RequestBody ReceptionistRequest request) {
        ReceptionistResponse response = receptionistService.createReceptionist(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'RECEPTIONIST')")
    public ResponseEntity<ReceptionistResponse> getReceptionistById(
            @PathVariable UUID id) {
        return ResponseEntity.ok(receptionistService.getReceptionistById(id));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'RECEPTIONIST')")
    public ResponseEntity<List<ReceptionistResponse>> getAllReceptionists() {
        return ResponseEntity.ok(receptionistService.getAllReceptionists());
    }

    @GetMapping("/by-account/{accountId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'RECEPTIONIST')")
    public ResponseEntity<ReceptionistResponse> getReceptionistByAccountId(@PathVariable UUID accountId) {
        return ResponseEntity.ok(receptionistService.getReceptionistByAccountId(accountId));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST')")
    public ResponseEntity<ReceptionistResponse> updateReceptionist(
            @PathVariable UUID id,
            @Valid @RequestBody ReceptionistRequest request) {
        return ResponseEntity.ok(receptionistService.updateReceptionist(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deleteReceptionist(
            @PathVariable UUID id) {
        receptionistService.deleteReceptionist(id);
        return ResponseEntity.ok("Receptionist profile deactivated successfully");
    }

    @PostMapping("/call-patient")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST')")
    public ResponseEntity<String> callPatient(@RequestParam UUID patientId, @RequestParam(required = false) String message) {
        receptionistService.callPatient(patientId, message);
        return ResponseEntity.ok("Patient call notification sent successfully");
    }

    @PostMapping("/register-walkin")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST')")
    public ResponseEntity<org.proj.dto.PatientResponse> registerWalkInPatient(
            @Valid @RequestBody org.proj.dto.WalkInPatientRegistrationRequest request) {
        org.proj.dto.PatientResponse response = receptionistService.registerWalkInPatient(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
}
