package org.proj.controller;

import org.proj.dto.*;
import org.proj.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    @Autowired
    private AdminService adminService;

    private UUID getCurrentAdminId() {
        org.proj.entity.UserEntity user = org.proj.security.SecurityUtils.getCurrentUser();
        if (user == null) {
            throw new RuntimeException("Unauthorized");
        }
        return user.getId();
    }

    @PutMapping("/doctors/{id}/verification")
    public ResponseEntity<DoctorResponse> updateDoctorVerification(
            @PathVariable UUID id,
            @Valid @RequestBody DoctorVerificationRequest request) {

        return ResponseEntity.ok(
                adminService.updateDoctorVerification(
                        id,
                        request,
                        getCurrentAdminId()));
    }

    @PutMapping("/doctors/{id}/status")
    public ResponseEntity<DoctorResponse> updateDoctorStatus(
            @PathVariable UUID id,
            @Valid @RequestBody StatusUpdateRequest request) {

        return ResponseEntity.ok(
                adminService.updateDoctorStatus(
                        id,
                        request,
                        getCurrentAdminId()));
    }

    @PutMapping("/hospitals/{id}/status")
    public ResponseEntity<HospitalResponse> updateHospitalStatus(
            @PathVariable UUID id,
            @Valid @RequestBody StatusUpdateRequest request) {

        return ResponseEntity.ok(
                adminService.updateHospitalStatus(
                        id,
                        request,
                        getCurrentAdminId()));
    }

    @PutMapping("/hospitals/{id}/verification")
    public ResponseEntity<HospitalResponse> updateHospitalVerification(
            @PathVariable UUID id,
            @Valid @RequestBody HospitalVerificationRequest request) {

        return ResponseEntity.ok(
                adminService.updateHospitalVerification(
                        id,
                        request,
                        getCurrentAdminId()));
    }

    @PutMapping("/users/{id}/block")
    public ResponseEntity<RegisterResponse> toggleUserBlock(
            @PathVariable UUID id,
            @RequestParam boolean block) {

        return ResponseEntity.ok(
                adminService.toggleUserBlock(
                        id,
                        block,
                        getCurrentAdminId()));
    }

    @GetMapping("/analytics")
    public ResponseEntity<AnalyticsResponse> getPlatformAnalytics() {

        return ResponseEntity.ok(
                adminService.getPlatformAnalytics());
    }

    @GetMapping("/dashboard/stats")
    public ResponseEntity<AdminDashboardStatsResponse> getDashboardStats() {

        return ResponseEntity.ok(
                adminService.getDashboardStats());
    }

    @GetMapping("/settings")
    public ResponseEntity<List<SystemSettingsResponse>> getAllSystemSettings() {

        return ResponseEntity.ok(
                adminService.getAllSystemSettings());
    }

    @PutMapping("/settings")
    public ResponseEntity<SystemSettingsResponse> updateSystemSetting(
            @Valid @RequestBody SystemSettingsRequest request) {

        return ResponseEntity.ok(
                adminService.updateSystemSetting(
                        request,
                        getCurrentAdminId()));
    }

    @GetMapping("/audit-logs")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse<AuditLogResponse>> getAuditLogs(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return ResponseEntity.ok(
                adminService.getAuditLogs(
                        search,
                        page,
                        size));
    }
}
