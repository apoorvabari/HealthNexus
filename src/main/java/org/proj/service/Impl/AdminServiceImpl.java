package org.proj.service.impl;

import org.proj.dto.*;
import org.proj.entity.*;
import org.proj.mapper.DoctorMapper;
import org.proj.mapper.HospitalMapper;
import org.proj.mapper.UserMapper;
import org.proj.repository.*;
import org.proj.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import java.util.UUID;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.regex.Pattern;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@Service
public class AdminServiceImpl implements AdminService {

    @Autowired
    private DoctorService doctorService;

    @Autowired
    private HospitalService hospitalService;

    @Autowired
    private UserService userService;

    @Autowired
    private AdminSystemSettingsRepo systemSettingsRepo;

    @Autowired
    private AdminAuditLogRepo auditLogRepo;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TenantContextService tenantContextService;

    @Autowired
    private DoctorMapper doctorMapper;

    @Autowired
    private HospitalMapper hospitalMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserRepo userRepo;

    @Autowired
    private DoctorRepo doctorRepo;

    @Autowired
    private PatientRepo patientRepo;

    @Autowired
    private AppointmentRepo appointmentRepo;

    @Autowired
    private HospitalRepo hospitalRepo;

    // =========================================================
    // PRIVATE HELPERS
    // =========================================================

    /**
     * Returns the HospitalEntity the current admin is authorized for.
     * Throws AccessDeniedException if no hospital context can be resolved
     * (e.g. admin has no assignment or the X-Hospital-Id header is missing/wrong).
     */
    private HospitalEntity requireAdminHospital() {
        UUID hospitalId = tenantContextService.getCurrentUserHospitalId();
        if (hospitalId == null) {
            throw new AccessDeniedException(
                    "No authorized hospital context found for the current admin");
        }
        return hospitalService.findHospitalById(hospitalId);
    }

    /**
     * Asserts that the current admin is authorized for the given hospitalId.
     * Throws AccessDeniedException otherwise.
     */
    private void assertHospitalAccess(UUID hospitalId) {
        if (!tenantContextService.hasCurrentUserHospitalAccess(hospitalId)) {
            throw new AccessDeniedException(
                    "You are not authorized to manage this hospital");
        }
    }

    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public AdminDashboardStatsResponse getDashboardStats() {
        UUID hospitalId = requireAdminHospital().getId();

        long totalDoctors = doctorRepo.countByHospitalId(hospitalId);
        long pendingDoctors = doctorRepo.countByHospitalIdAndVerificationStatus(hospitalId, DoctorEntity.VerificationStatus.PENDING);
        long approvedDoctors = doctorRepo.countByHospitalIdAndVerificationStatus(hospitalId, DoctorEntity.VerificationStatus.APPROVED);
        long rejectedDoctors = doctorRepo.countByHospitalIdAndVerificationStatus(hospitalId, DoctorEntity.VerificationStatus.REJECTED);

        long totalUsers = userRepo.countByHospitalId(hospitalId);
        long activeUsers = userRepo.countActiveByHospitalId(hospitalId);

        return AdminDashboardStatsResponse.builder()
                .totalDoctors(totalDoctors)
                .pendingDoctors(pendingDoctors)
                .approvedDoctors(approvedDoctors)
                .rejectedDoctors(rejectedDoctors)
                .totalHospitals(1)
                .pendingHospitals(hospitalRepo.countByIdAndVerificationStatus(hospitalId, HospitalEntity.VerificationStatus.PENDING))
                .approvedHospitals(hospitalRepo.countByIdAndVerificationStatus(hospitalId, HospitalEntity.VerificationStatus.APPROVED))
                .rejectedHospitals(hospitalRepo.countByIdAndVerificationStatus(hospitalId, HospitalEntity.VerificationStatus.REJECTED))
                .totalUsers(totalUsers)
                .activeUsers(activeUsers)
                .deletedUsers(userRepo.countDeletedByHospitalId(hospitalId))
                .build();
    }

    @Override
    @Transactional
    public HospitalResponse updateHospitalVerification(
            UUID hospitalId,
            HospitalVerificationRequest request,
            UUID adminId) {

        // Fix 4: enforce tenant scope — 403 if admin doesn't own this hospital
        assertHospitalAccess(hospitalId);

        HospitalEntity hospital =
                hospitalService.findHospitalById(hospitalId);

    if (request == null || request.getVerificationStatus() == null) {
        throw new IllegalArgumentException(
                "Verification status is required"
        );
    }

    boolean detailsVerified =
            Boolean.TRUE.equals(request.getDetailsVerified());

    boolean locationVerified =
            Boolean.TRUE.equals(request.getLocationVerified());

    hospital.setDetailsVerified(detailsVerified);
    hospital.setLocationVerified(locationVerified);

    if (request.getVerificationStatus()
            == HospitalEntity.VerificationStatus.APPROVED) {

        if (!detailsVerified || !locationVerified) {

            throw new IllegalArgumentException(
                    "Clinic cannot be approved until clinic details and location are verified"
            );
        }

        hospital.setVerificationStatus(
                HospitalEntity.VerificationStatus.APPROVED
        );

        hospital.setVerificationRemarks(
                request.getVerificationRemarks()
        );

        hospital.setVerifiedBy(adminId);
        hospital.setVerifiedAt(java.time.LocalDateTime.now());

        hospitalService.save(hospital);

        logAuditAction(
                "APPROVE_CLINIC",
                "HospitalEntity",
                hospitalId,
                adminId,
                "Clinic approved after details and location verification",
                hospital
        );

    } else if (
            request.getVerificationStatus()
                    == HospitalEntity.VerificationStatus.REJECTED) {

        if (request.getVerificationRemarks() == null
                || request.getVerificationRemarks().trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Verification remarks are required when rejecting a clinic"
            );
        }

        hospital.setVerificationStatus(
                HospitalEntity.VerificationStatus.REJECTED
        );

        hospital.setVerificationRemarks(
                request.getVerificationRemarks().trim()
        );

        hospital.setVerifiedBy(adminId);
        hospital.setVerifiedAt(
                java.time.LocalDateTime.now()
        );

        hospitalService.save(hospital);

        logAuditAction(
                "REJECT_CLINIC",
                "HospitalEntity",
                hospitalId,
                adminId,
                "Clinic rejected: "
                        + request.getVerificationRemarks().trim(),
                hospital
        );

    } else {

        hospital.setVerificationStatus(
                HospitalEntity.VerificationStatus.PENDING
        );

        hospital.setVerificationRemarks(
                request.getVerificationRemarks()
        );

        hospital.setVerifiedBy(null);
        hospital.setVerifiedAt(null);

        hospitalService.save(hospital);

        logAuditAction(
                "VERIFY_CLINIC",
                "HospitalEntity",
                hospitalId,
                adminId,
                "Clinic verification progress updated",
                hospital
        );
    }

    return hospitalMapper.toResponse(hospital);
}

    @Override
    @Transactional
    public DoctorResponse updateDoctorVerification(
            UUID doctorId,
            DoctorVerificationRequest request,
            UUID adminId) {

        // Fix 3: resolve the admin's hospital first, then do a tenant-scoped lookup
        HospitalEntity adminHospital = requireAdminHospital();
        
        DoctorEntity doctor = doctorService.findDoctorById(doctorId);
        if (doctor.getHospital() == null || !doctor.getHospital().getId().equals(adminHospital.getId())) {
            throw new AccessDeniedException("You are not authorized to manage this doctor");
        }

        if (request == null || request.getVerificationStatus() == null) {
            throw new IllegalArgumentException(
                    "Verification status is required");
        }

        boolean licenseVerified = Boolean.TRUE.equals(request.getLicenseVerified());

        boolean degreeVerified = Boolean.TRUE.equals(request.getDegreeVerified());

        boolean specializationVerified = Boolean.TRUE.equals(request.getSpecializationVerified());

        doctor.setLicenseVerified(licenseVerified);
        doctor.setDegreeVerified(degreeVerified);
        doctor.setSpecializationVerified(specializationVerified);

        if (request.getVerificationStatus() == DoctorEntity.VerificationStatus.APPROVED) {

            if (!licenseVerified
                    || !degreeVerified
                    || !specializationVerified) {

                throw new IllegalArgumentException(
                        "Doctor cannot be approved until license, degree and specialization are verified");
            }

            doctor.setVerificationStatus(
                    DoctorEntity.VerificationStatus.APPROVED);

            doctor.setStatus(DoctorEntity.DoctorStatus.ACTIVE);

            doctor.setVerificationRemarks(
                    request.getVerificationRemarks());

            doctor.setVerifiedBy(adminId);
            doctor.setVerifiedAt(java.time.LocalDateTime.now());

            doctorService.save(doctor);

            logAuditAction(
                    "APPROVE_DOCTOR",
                    "DoctorEntity",
                    doctorId,
                    adminId,
                    "Doctor approved after license, degree and specialization verification",
                    adminHospital);
        }

        else if (request.getVerificationStatus() == DoctorEntity.VerificationStatus.REJECTED) {

            if (request.getVerificationRemarks() == null
                    || request.getVerificationRemarks().trim().isEmpty()) {

                throw new IllegalArgumentException(
                        "Verification remarks are required when rejecting a doctor");
            }

            doctor.setVerificationStatus(
                    DoctorEntity.VerificationStatus.REJECTED);

            doctor.setStatus(DoctorEntity.DoctorStatus.INACTIVE);

            doctor.setVerificationRemarks(
                    request.getVerificationRemarks().trim());

            doctor.setVerifiedBy(adminId);
            doctor.setVerifiedAt(java.time.LocalDateTime.now());

            doctorService.save(doctor);

            logAuditAction(
                    "REJECT_DOCTOR",
                    "DoctorEntity",
                    doctorId,
                    adminId,
                    "Doctor rejected: "
                            + request.getVerificationRemarks().trim(),
                    adminHospital);
        }

        else {

            doctor.setVerificationStatus(
                    DoctorEntity.VerificationStatus.PENDING);

            doctor.setVerificationRemarks(
                    request.getVerificationRemarks());

            doctor.setVerifiedBy(null);
            doctor.setVerifiedAt(null);

            doctorService.save(doctor);

            logAuditAction(
                    "VERIFY_DOCTOR",
                    "DoctorEntity",
                    doctorId,
                    adminId,
                    "Doctor verification progress updated",
                    adminHospital);
        }

        return doctorMapper.toResponse(doctor);
    }

    @Override
    @Transactional
    public DoctorResponse updateDoctorStatus(UUID doctorId, StatusUpdateRequest request, UUID adminId) {
        // Fix 3: tenant-scoped doctor lookup
        HospitalEntity adminHospital = requireAdminHospital();
        DoctorEntity doctor = doctorService.findDoctorById(doctorId);
        if (doctor.getHospital() == null || !doctor.getHospital().getId().equals(adminHospital.getId())) {
            throw new AccessDeniedException("You are not authorized to manage this doctor");
        }

        try {
            DoctorEntity.DoctorStatus newStatus = DoctorEntity.DoctorStatus.valueOf(request.getStatus().toUpperCase());
            
            if (newStatus == DoctorEntity.DoctorStatus.ACTIVE) {
                if (doctor.getVerificationStatus() == DoctorEntity.VerificationStatus.PENDING) {
                    throw new IllegalArgumentException("Cannot activate a doctor whose verification status is PENDING.");
                }
                if (doctor.getVerificationStatus() == DoctorEntity.VerificationStatus.REJECTED) {
                    throw new IllegalArgumentException("Cannot activate a doctor whose verification status is REJECTED.");
                }
            } else if (newStatus == DoctorEntity.DoctorStatus.SUSPENDED) {
                if (doctor.getVerificationStatus() != DoctorEntity.VerificationStatus.APPROVED) {
                    throw new IllegalArgumentException("Cannot suspend a doctor who is not APPROVED.");
                }
            }
            
            doctor.setStatus(newStatus);
            doctorService.save(doctor);

            logAuditAction("UPDATE_DOCTOR_STATUS", "DoctorEntity", doctorId, adminId, "Status updated to " + newStatus, adminHospital);

            return doctorMapper.toResponse(doctor);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(e.getMessage() != null && e.getMessage().contains("Cannot") ? e.getMessage() : "Invalid status provided for doctor");
        }
    }

    @Override
    @Transactional
    public HospitalResponse updateHospitalStatus(UUID hospitalId, StatusUpdateRequest request, UUID adminId) {
        // Fix 4: enforce tenant scope — 403 if admin doesn't own this hospital
        assertHospitalAccess(hospitalId);
        HospitalEntity hospital = hospitalService.findHospitalById(hospitalId);

        try {
            HospitalEntity.HospitalStatus newStatus = HospitalEntity.HospitalStatus
                    .valueOf(request.getStatus().toUpperCase());
            hospital.setStatus(newStatus);
            hospitalService.save(hospital);

            logAuditAction("UPDATE_HOSPITAL_STATUS", "HospitalEntity", hospitalId, adminId,
                    "Status updated to " + newStatus, hospital);

            return hospitalMapper.toResponse(hospital);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid status provided for hospital");
        }
    }

    @Override
    @Transactional
    public RegisterResponse toggleUserBlock(UUID userId, boolean block, UUID adminId) {
        HospitalEntity adminHospital = requireAdminHospital();
        if (!userRepo.existsByIdAndHospitalId(userId, adminHospital.getId())) {
            throw new AccessDeniedException("User does not belong to the current hospital");
        }
        UserEntity user = userService.findUserById(userId);

        user.setIsActive(!block);
        userService.save(user);

        String action = block ? "BLOCK_USER" : "UNBLOCK_USER";
        logAuditAction(action, "UserEntity", userId, adminId, "User account " + (block ? "blocked" : "unblocked"), adminHospital);

        return userMapper.toResponse(user);
    }

    @Override
    public AnalyticsResponse getPlatformAnalytics() {
        UUID hospitalId = requireAdminHospital().getId();
        return AnalyticsResponse.builder()
                .totalDoctors(doctorRepo.countByHospitalId(hospitalId))
                .totalPatients(patientRepo.countByHospitalId(hospitalId))
                .totalHospitals(1)
                .totalAppointments(appointmentRepo.countByHospitalId(hospitalId))
                .appointmentsToday(appointmentRepo.countByAppointmentDateAndHospitalId(LocalDate.now(), hospitalId))
                .build();
    }

    @Override
    @Transactional
    public SystemSettingsResponse updateSystemSetting(SystemSettingsRequest request, UUID adminId) {
        if (request == null) {
            throw new IllegalArgumentException("Setting request cannot be null");
        }

        if (request.getSettingKey() == null || request.getSettingKey().trim().isEmpty()) {
            throw new IllegalArgumentException("Setting key cannot be empty");
        }

        if (request.getSettingValue() == null || request.getSettingValue().trim().isEmpty()) {
            throw new IllegalArgumentException("Setting value cannot be empty");
        }

        String settingKey = request.getSettingKey().trim();
        String settingValue = request.getSettingValue().trim();

        JsonNode settingsJson;
        try {
            settingsJson = objectMapper.readTree(settingValue);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Setting value must contain valid JSON");
        }

        switch (settingKey) {
            case "GENERAL_SETTINGS":
                validateGeneralSettings(settingsJson);
                break;
            case "USER_ACCOUNT_SETTINGS":
                
                break;
            case "ROLE_SETTINGS":
                break;
            
        }

        HospitalEntity adminHospital = requireAdminHospital();
        AdminSystemSettingsEntity setting = systemSettingsRepo.findByHospitalIdAndSettingKey(adminHospital.getId(), settingKey)
                .orElseGet(() -> {
                    AdminSystemSettingsEntity s = new AdminSystemSettingsEntity();
                    s.setHospital(adminHospital);
                    return s;
                });

        setting.setSettingKey(settingKey);
        setting.setSettingValue(settingValue);
        setting.setHospital(adminHospital);

        String desc = request.getDescription();
        if (desc == null || desc.isBlank()) {
            desc = "Managed by Admin Portal Settings";
        }
        setting.setDescription(desc);

        AdminSystemSettingsEntity savedSetting = systemSettingsRepo.save(setting);
        logAuditAction("UPDATE_SETTING", "AdminSystemSettingsEntity", savedSetting.getId(), adminId,
                "Updated setting: " + request.getSettingKey(), adminHospital);

        return SystemSettingsResponse.builder()
                .id(savedSetting.getId())
                .settingKey(savedSetting.getSettingKey())
                .settingValue(savedSetting.getSettingValue())
                .description(savedSetting.getDescription())
                .updatedAt(savedSetting.getUpdatedAt())
                .build();
    }

    private void validateGeneralSettings(JsonNode json) {
        if (!json.hasNonNull("appName") || json.get("appName").asText().trim().isEmpty()) {
            throw new IllegalArgumentException("Application name is required");
        }
        String appName = json.get("appName").asText().trim();
        if (appName.length() > 100) {
            throw new IllegalArgumentException("Application name cannot exceed 100 characters");
        }

        if (!json.hasNonNull("supportEmail") || json.get("supportEmail").asText().trim().isEmpty()) {
            throw new IllegalArgumentException("Support email is required");
        }
        String email = json.get("supportEmail").asText().trim();
        if (!isValidEmail(email)) {
            throw new IllegalArgumentException("Support email must be a valid email address");
        }

        if (json.hasNonNull("supportPhone")) {
            String phone = json.get("supportPhone").asText().trim();
            if (!phone.isEmpty() && !phone.matches("^[0-9+()\\\\-\\\\s]{7,20}$")) {
                throw new IllegalArgumentException("Support phone number is invalid");
            }
        }

        if (!json.hasNonNull("language") || json.get("language").asText().trim().isEmpty()) {
            throw new IllegalArgumentException("Language is required");
        }
    }

    private boolean isValidEmail(String email) {
        return Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$").matcher(email).matches();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SystemSettingsResponse> getAllSystemSettings() {
        HospitalEntity adminHospital = requireAdminHospital();
        return systemSettingsRepo.findAllByHospitalId(adminHospital.getId()).stream()
                .map(setting -> SystemSettingsResponse.builder()
                        .id(setting.getId())
                        .settingKey(setting.getSettingKey())
                        .settingValue(setting.getSettingValue())
                        .description(setting.getDescription())
                        .updatedAt(setting.getUpdatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> getAuditLogs(String search, int page, int size) {
        UUID hospitalId = tenantContextService.getCurrentUserHospitalId();
        if (hospitalId == null) {
            throw new AccessDeniedException("Tenant hospital context is required");
        }
        Page<AdminAuditLogEntity> logPage = auditLogRepo.searchLogs(hospitalId, search,
                PageRequest.of(page, size, Sort.by("timestamp").descending()));

        List<AuditLogResponse> content = logPage.getContent().stream()
                .map(log -> new AuditLogResponse(
                        log.getId(),
                        log.getAction(),
                        log.getEntityName(),
                        log.getEntityId(),
                        log.getPerformedBy(),
                        log.getDetails(),
                        log.getTimestamp()))
                .toList();
        return new PageResponse<>(content, logPage.getNumber(), logPage.getSize(), logPage.getTotalElements(),
                logPage.getTotalPages(), logPage.isLast());
    }

    /**
     * Fix 2: hospital is now required so the DB NOT NULL constraint is satisfied.
     * Every mutation that creates an audit entry must resolve and pass the admin's hospital.
     */
    private void logAuditAction(
            String action,
            String entityName,
            UUID entityId,
            UUID adminId,
            String details,
            HospitalEntity hospital) {

        AdminAuditLogEntity auditLog = AdminAuditLogEntity.builder()
                .action(action)
                .entityName(entityName)
                .entityId(entityId)
                .performedBy(adminId)
                .details(details)
                .hospital(hospital)
                .build();
        auditLogRepo.save(auditLog);
    }
}
