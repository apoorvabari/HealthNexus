package org.proj.controller;

import org.proj.service.DoctorPresenceService;
import org.proj.service.TenantContextService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/doctors")
public class DoctorPresenceController {

    @Autowired
    private DoctorPresenceService presenceService;

    @Autowired
    private TenantContextService tenantContextService;

    @Autowired
    private org.proj.repository.DoctorRepo doctorRepo;

    @GetMapping("/presence")
    @PreAuthorize("hasAnyRole('PATIENT', 'ADMIN', 'RECEPTIONIST', 'DOCTOR')")
    public ResponseEntity<Set<UUID>> getOnlineDoctors() {
        UUID hospitalId = tenantContextService.getCurrentUserHospitalId();
        return ResponseEntity.ok(presenceService.getOnlineDoctors(hospitalId));
    }

    @org.springframework.web.bind.annotation.PatchMapping("/presence/status")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<Void> updateMyPresenceStatus(@org.springframework.web.bind.annotation.RequestParam boolean isOnline) {
        org.proj.entity.UserEntity currentUser = org.proj.security.SecurityUtils.getCurrentUser();
        if (currentUser == null) {
            throw new org.springframework.security.access.AccessDeniedException("Unauthorized");
        }
        org.proj.entity.DoctorEntity doctor = doctorRepo.findByAccountId(currentUser.getId())
                .orElseThrow(() -> new IllegalArgumentException("Doctor profile not found"));

        if (doctor.getHospital() != null && doctor.getHospital().getId() != null) {
            presenceService.updateDoctorPresenceStatus(doctor.getId(), doctor.getHospital().getId(), isOnline);
        }

        return ResponseEntity.ok().build();
    }
}
