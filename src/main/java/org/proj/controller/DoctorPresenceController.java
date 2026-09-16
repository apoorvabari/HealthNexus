package org.proj.controller;

import org.proj.service.DoctorPresenceService;
import org.proj.service.TenantContextService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
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

    @GetMapping("/presence")
    @PreAuthorize("hasAnyRole('PATIENT', 'ADMIN', 'RECEPTIONIST', 'DOCTOR')")
    public ResponseEntity<Set<UUID>> getOnlineDoctors() {
        UUID hospitalId = tenantContextService.getCurrentUserHospitalId();
        if (hospitalId == null) {
            throw new AccessDeniedException("Hospital context is required");
        }
        return ResponseEntity.ok(presenceService.getOnlineDoctors(hospitalId));
    }
}
