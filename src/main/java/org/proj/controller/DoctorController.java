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
@RequestMapping({"/api/doctors", "/doctors"})
public class DoctorController {

    @Autowired
    private DoctorService doctorService;

    @Autowired
    private org.proj.service.DoctorHospitalAffiliationService affiliationService;

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

    @GetMapping("/department/{departmentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'RECEPTIONIST', 'PATIENT')")
    public ResponseEntity<java.util.List<DoctorResponse>> getDoctorsByDepartment(
            @PathVariable UUID departmentId) {
        return ResponseEntity.ok(doctorService.getDoctorsByDepartment(departmentId));
    }

    @GetMapping("/hospital/{hospitalId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'RECEPTIONIST', 'PATIENT')")
    public ResponseEntity<java.util.List<DoctorResponse>> getDoctorsByHospital(
            @PathVariable UUID hospitalId) {
        return ResponseEntity.ok(doctorService.getDoctorsByHospital(hospitalId));
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
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_RECEPTIONIST"));
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

    @PutMapping("/active-hospital/{hospitalId}")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public ResponseEntity<java.util.Map<String, Object>> switchActiveHospital(
            @PathVariable UUID hospitalId,
            @RequestParam(required = false) UUID doctorId) {
        org.proj.entity.UserEntity currentUser = org.proj.security.SecurityUtils.getCurrentUser();
        UUID effectiveDoctorId = doctorId;
        if (effectiveDoctorId == null) {
            org.proj.entity.DoctorEntity doctor = doctorService.findDoctorEntityByAccountId(currentUser.getId())
                    .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Doctor profile not found for user"));
            effectiveDoctorId = doctor.getId();
        }
        doctorService.switchActiveHospital(effectiveDoctorId, hospitalId);
        return ResponseEntity.ok(java.util.Map.of(
                "message", "Active duty hospital updated successfully",
                "activeHospitalId", hospitalId
        ));
    }

    @GetMapping("/my-affiliations")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public ResponseEntity<java.util.List<org.proj.dto.DoctorAffiliationResponse>> getMyAffiliations(
            @RequestParam(required = false) UUID doctorId) {
        org.proj.entity.UserEntity currentUser = org.proj.security.SecurityUtils.getCurrentUser();
        UUID effectiveDoctorId = doctorId;
        if (effectiveDoctorId == null) {
            org.proj.entity.DoctorEntity doctor = doctorService.findDoctorEntityByAccountId(currentUser.getId())
                    .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Doctor profile not found for user"));
            effectiveDoctorId = doctor.getId();
        }
        return ResponseEntity.ok(doctorService.getDoctorAffiliations(effectiveDoctorId));
    }

    @GetMapping("/{id}/affiliations")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'RECEPTIONIST')")
    public ResponseEntity<java.util.List<org.proj.dto.DoctorAffiliationResponse>> getDoctorAffiliations(
            @PathVariable UUID id) {
        return ResponseEntity.ok(doctorService.getDoctorAffiliations(id));
    }

    @PostMapping("/{id}/affiliations")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<org.proj.dto.DoctorAffiliationResponse> addOrUpdateAffiliation(
            @PathVariable UUID id,
            @Valid @RequestBody org.proj.dto.DoctorAffiliationRequest request) {
        return ResponseEntity.ok(doctorService.addOrUpdateAffiliation(id, request));
    }

    @DeleteMapping("/affiliations/{affiliationId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<java.util.Map<String, String>> removeAffiliation(
            @PathVariable UUID affiliationId) {
        doctorService.removeAffiliation(affiliationId);
        return ResponseEntity.ok(java.util.Map.of("message", "Affiliation removed successfully"));
    }

    // --- Doctor Affiliation Request & Admin Review Endpoints ---

    @PostMapping("/affiliation-requests")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<org.proj.dto.DoctorAffiliationApplicationResponse> submitAffiliationRequest(
            @Valid @RequestBody org.proj.dto.DoctorAffiliationApplicationRequest request) {
        return new ResponseEntity<>(affiliationService.submitAffiliationRequest(request), HttpStatus.CREATED);
    }

    @GetMapping("/my-affiliation-requests")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<java.util.List<org.proj.dto.DoctorAffiliationApplicationResponse>> getMyAffiliationRequests() {
        return ResponseEntity.ok(affiliationService.getMyAffiliationRequests());
    }

    @GetMapping("/admin/affiliation-requests")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<java.util.List<org.proj.dto.DoctorAffiliationApplicationResponse>> getPendingRequestsForAdmin(
            @RequestParam(required = false) UUID hospitalId,
            @RequestParam(required = false) org.proj.entity.DoctorAffiliationRequestEntity.RequestStatus status) {
        return ResponseEntity.ok(affiliationService.getPendingRequestsForAdmin(hospitalId, status));
    }

    @PutMapping("/admin/affiliation-requests/{requestId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<org.proj.dto.DoctorAffiliationApplicationResponse> reviewAffiliationRequest(
            @PathVariable UUID requestId,
            @Valid @RequestBody org.proj.dto.ReviewAffiliationRequest review) {
        return ResponseEntity.ok(affiliationService.reviewAffiliationRequest(requestId, review));
    }
}
