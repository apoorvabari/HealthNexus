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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/doctors")
@CrossOrigin(origins = "http://localhost:8082")
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

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'RECEPTIONIST', 'PATIENT')")
    public ResponseEntity<DoctorResponse> getDoctorById(
            @PathVariable UUID id) {
        return ResponseEntity.ok(doctorService.getDoctorById(id));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'RECEPTIONIST', 'PATIENT')")
    public ResponseEntity<List<DoctorResponse>> getAllDoctors() {
        return ResponseEntity.ok(doctorService.getAllDoctors());
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
