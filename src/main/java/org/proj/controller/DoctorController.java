package org.proj.controller;

import jakarta.validation.Valid;
import org.proj.dto.DoctorRequest;
import org.proj.dto.DoctorResponse;
import org.proj.service.DoctorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.proj.dto.PageResponse;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/doctors")

public class DoctorController {

    @Autowired
    private DoctorService doctorService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    public ResponseEntity<DoctorResponse> createDoctor(
            @Valid @RequestBody DoctorRequest request) {
        DoctorResponse response = doctorService.createDoctor(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/patient/search")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<PageResponse<DoctorResponse>> searchDoctorsForPatient(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String specialization,
            @RequestParam(required = false) UUID hospitalId,
            @RequestParam(required = false) UUID departmentId,
            @RequestParam(required = false) BigDecimal maxFee,
            @RequestParam(required = false) Integer minExperience,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return ResponseEntity.ok(
                doctorService.searchDoctorsForPatient(
                        search,
                        specialization,
                        hospitalId,
                        departmentId,
                        maxFee,
                        minExperience,
                        page,
                        size));
    }

    @GetMapping("/nearby")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<PageResponse<DoctorResponse>> getNearbyDoctors(
            @RequestParam double latitude,
            @RequestParam double longitude,
            @RequestParam(defaultValue = "10.0") double radiusKm,
            @RequestParam(required = false) String specialization,
            @RequestParam(required = false) BigDecimal maxFee,
            @RequestParam(required = false) Integer minExperience,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return ResponseEntity.ok(
                doctorService.getNearbyDoctors(
                        latitude,
                        longitude,
                        radiusKm,
                        specialization,
                        maxFee,
                        minExperience,
                        page,
                        size));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'RECEPTIONIST', 'PATIENT')")
    public ResponseEntity<DoctorResponse> getDoctorById(
            @PathVariable UUID id) {
        return ResponseEntity.ok(doctorService.getDoctorById(id));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'RECEPTIONIST', 'PATIENT')")
    public ResponseEntity<PageResponse<DoctorResponse>> getAllDoctors(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            org.springframework.security.core.Authentication authentication) {
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        return ResponseEntity.ok(doctorService.getAllDoctors(search, page, size, isAdmin));
    }

    @GetMapping("/by-account/{accountId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'RECEPTIONIST', 'PATIENT')")
    public ResponseEntity<DoctorResponse> getDoctorByAccountId(@PathVariable UUID accountId) {
        return ResponseEntity.ok(doctorService.getDoctorByAccountId(accountId));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    public ResponseEntity<DoctorResponse> updateDoctor(
            @PathVariable UUID id,
            @Valid @RequestBody DoctorRequest request) {
        return ResponseEntity.ok(doctorService.updateDoctor(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deleteDoctor(
            @PathVariable UUID id) {
        doctorService.deleteDoctor(id);
        return ResponseEntity.ok("Doctor profile deactivated successfully");
    }

}
