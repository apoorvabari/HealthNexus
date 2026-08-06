package org.proj.controller;

import jakarta.validation.Valid;
import org.proj.dto.HospitalRequest;
import org.proj.dto.HospitalResponse;
import org.proj.service.HospitalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/hospitals")
@CrossOrigin(origins = "http://localhost:8082")
public class HospitalController {

    @Autowired
    private HospitalService hospitalService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<HospitalResponse> createHospital(
            @Valid @RequestBody HospitalRequest request) {
        HospitalResponse response = hospitalService.createHospital(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'RECEPTIONIST', 'PATIENT')")
    public ResponseEntity<HospitalResponse> getHospitalById(
            @PathVariable UUID id) {
        return ResponseEntity.ok(hospitalService.getHospitalById(id));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'RECEPTIONIST', 'PATIENT')")
    public ResponseEntity<List<HospitalResponse>> getAllHospitals() {
        return ResponseEntity.ok(hospitalService.getAllHospitals());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<HospitalResponse> updateHospital(
            @PathVariable UUID id,
            @Valid @RequestBody HospitalRequest request) {
        return ResponseEntity.ok(hospitalService.updateHospital(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deleteHospital(
            @PathVariable UUID id) {
        hospitalService.deleteHospital(id);
        return ResponseEntity.ok("Hospital deactivated successfully");
    }
}
