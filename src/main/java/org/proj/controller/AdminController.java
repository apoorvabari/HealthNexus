package org.proj.controller;

import org.proj.dto.*;
import org.proj.entity.UserEntity;
import org.proj.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.proj.dto.PageResponse;
import org.proj.dto.AuditLogResponse;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    @Autowired
    private AdminService adminService;

    private UUID getAdminId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof UserEntity) {
            return ((UserEntity) authentication.getPrincipal()).getId();
        }
        throw new IllegalStateException("Not authenticated as a valid user");
    }

    @PutMapping("/doctors/{id}/status")
    public ResponseEntity<DoctorResponse> updateDoctorStatus(
            @PathVariable UUID id,
            @Valid @RequestBody StatusUpdateRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(adminService.updateDoctorStatus(id, request, getAdminId(authentication)));
    }

    @PutMapping("/hospitals/{id}/status")
    public ResponseEntity<HospitalResponse> updateHospitalStatus(
            @PathVariable UUID id,
            @Valid @RequestBody StatusUpdateRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(adminService.updateHospitalStatus(id, request, getAdminId(authentication)));
    }

    @PutMapping("/users/{id}/toggle-block")
    public ResponseEntity<RegisterResponse> toggleUserBlock(
            @PathVariable UUID id,
            @RequestParam boolean block,
            Authentication authentication) {
        return ResponseEntity.ok(adminService.toggleUserBlock(id, block, getAdminId(authentication)));
    }

    @GetMapping("/analytics")
    public ResponseEntity<AnalyticsResponse> getPlatformAnalytics() {
        return ResponseEntity.ok(adminService.getPlatformAnalytics());
    }

    @GetMapping("/settings")
    public ResponseEntity<List<SystemSettingsResponse>> getAllSystemSettings() {
        return ResponseEntity.ok(adminService.getAllSystemSettings());
    }

    @PutMapping("/settings")
    public ResponseEntity<SystemSettingsResponse> updateSystemSetting(
            @Valid @RequestBody SystemSettingsRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(adminService.updateSystemSetting(request, getAdminId(authentication)));
    }

    @GetMapping("/audit-logs")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse<AuditLogResponse>> getAuditLogs(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(adminService.getAuditLogs(search, page, size));
    }
}
