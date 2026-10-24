package org.proj.service;

import org.proj.dto.*;

import java.util.List;
import java.util.UUID;
import java.util.Optional;

import org.proj.entity.AdminEntity;


public interface AdminService {

    DoctorResponse updateDoctorStatus(
            UUID doctorId,
            StatusUpdateRequest request,
            UUID adminId);

    DoctorResponse updateDoctorVerification(
            UUID doctorId,
            DoctorVerificationRequest request,
            UUID adminId);

    HospitalResponse updateHospitalStatus(
            UUID hospitalId,
            StatusUpdateRequest request,
            UUID adminId);

    HospitalResponse updateHospitalVerification(
            UUID hospitalId,
            HospitalVerificationRequest request,
            UUID adminId);

    RegisterResponse toggleUserBlock(
            UUID userId,
            boolean block,
            UUID adminId);

    AnalyticsResponse getPlatformAnalytics();

    AdminDashboardStatsResponse getDashboardStats();

    SystemSettingsResponse updateSystemSetting(
            SystemSettingsRequest request,
            UUID adminId);

    List<SystemSettingsResponse> getAllSystemSettings();

    PageResponse<AuditLogResponse> getAuditLogs(
            String search,
            int page,
            int size);

    Optional<AdminEntity> findAdminEntityByAccountId(UUID accountId);

    List<AdminEntity> findAdminsByHospitalId(UUID hospitalId);

    List<UUID> getActiveHospitalIdsForAdmin(UUID accountId);

    boolean existsByAccountIdAndHospitalId(UUID accountId, UUID hospitalId);
}
