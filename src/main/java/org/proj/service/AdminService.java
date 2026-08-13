package org.proj.service;

import org.proj.dto.*;

import java.util.List;
import java.util.UUID;

import org.proj.dto.PageResponse;

public interface AdminService {
    
    // Doctor Management
    DoctorResponse updateDoctorStatus(UUID doctorId, StatusUpdateRequest request, UUID adminId);
    
    // Hospital Management
    HospitalResponse updateHospitalStatus(UUID hospitalId, StatusUpdateRequest request, UUID adminId);
    
    // User Management
    RegisterResponse toggleUserBlock(UUID userId, boolean block, UUID adminId);
    
    // Analytics
    AnalyticsResponse getPlatformAnalytics();
    
    // System Settings
    SystemSettingsResponse updateSystemSetting(SystemSettingsRequest request, UUID adminId);
    List<SystemSettingsResponse> getAllSystemSettings();
    
    // Audit Logs
    PageResponse<AuditLogResponse> getAuditLogs(String search, int page, int size);
}
