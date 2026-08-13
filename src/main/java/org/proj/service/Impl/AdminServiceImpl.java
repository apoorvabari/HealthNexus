package org.proj.service.Impl;

import org.proj.dto.*;
import org.proj.entity.*;
import org.proj.mapper.DoctorMapper;
import org.proj.mapper.HospitalMapper;
import org.proj.mapper.UserMapper;
import org.proj.repository.*;
import org.proj.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.proj.dto.PageResponse;
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
    private PatientService patientService;

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private AdminSystemSettingsRepo systemSettingsRepo;

    @Autowired
    private AdminAuditLogRepo auditLogRepo;

    @Autowired
    private DoctorMapper doctorMapper;

    @Autowired
    private HospitalMapper hospitalMapper;

    @Autowired
    private UserMapper userMapper;

    @Override
    @Transactional
    public DoctorResponse updateDoctorStatus(UUID doctorId, StatusUpdateRequest request, UUID adminId) {
        DoctorEntity doctor = doctorService.findDoctorById(doctorId);

        try {
            DoctorEntity.DoctorStatus newStatus = DoctorEntity.DoctorStatus.valueOf(request.getStatus().toUpperCase());
            doctor.setStatus(newStatus);
            doctorService.save(doctor);

            logAuditAction("UPDATE_DOCTOR_STATUS", "DoctorEntity", doctorId, adminId, "Status updated to " + newStatus);

            return doctorMapper.toResponse(doctor);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid status provided for doctor");
        }
    }

    @Override
    @Transactional
    public HospitalResponse updateHospitalStatus(UUID hospitalId, StatusUpdateRequest request, UUID adminId) {
        HospitalEntity hospital = hospitalService.findHospitalById(hospitalId);

        try {
            HospitalEntity.HospitalStatus newStatus = HospitalEntity.HospitalStatus.valueOf(request.getStatus().toUpperCase());
            hospital.setStatus(newStatus);
            hospitalService.save(hospital);

            logAuditAction("UPDATE_HOSPITAL_STATUS", "HospitalEntity", hospitalId, adminId, "Status updated to " + newStatus);

            return hospitalMapper.toResponse(hospital);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid status provided for hospital");
        }
    }

    @Override
    @Transactional
    public RegisterResponse toggleUserBlock(UUID userId, boolean block, UUID adminId) {
        UserEntity user = userService.findUserById(userId);

        user.setIsActive(!block);
        userService.save(user);

        String action = block ? "BLOCK_USER" : "UNBLOCK_USER";
        logAuditAction(action, "UserEntity", userId, adminId, "User account " + (block ? "blocked" : "unblocked"));

        return userMapper.toResponse(user);
    }

    @Override
    public AnalyticsResponse getPlatformAnalytics() {
        long totalDoctors = doctorService.count();
        long totalPatients = patientService.count();
        long totalHospitals = hospitalService.count();
        long totalAppointments = appointmentService.count();
        
        // Count today's appointments
        long appointmentsToday = appointmentService.countByAppointmentDate(LocalDate.now());
        // Note: adjust the method name above if your AppointmentRepo uses a different query for dates

        return AnalyticsResponse.builder()
                .totalDoctors(totalDoctors)
                .totalPatients(totalPatients)
                .totalHospitals(totalHospitals)
                .totalAppointments(totalAppointments)
                .appointmentsToday(appointmentsToday)
                .build();
    }

    @Override
    @Transactional
    public SystemSettingsResponse updateSystemSetting(SystemSettingsRequest request, UUID adminId) {
        Optional<AdminSystemSettingsEntity> existingSettingOpt = systemSettingsRepo.findBySettingKey(request.getSettingKey());

        AdminSystemSettingsEntity setting;
        if (existingSettingOpt.isPresent()) {
            setting = existingSettingOpt.get();
            setting.setSettingValue(request.getSettingValue());
            if (request.getDescription() != null) {
                setting.setDescription(request.getDescription());
            }
        } else {
            setting = AdminSystemSettingsEntity.builder()
                    .settingKey(request.getSettingKey())
                    .settingValue(request.getSettingValue())
                    .description(request.getDescription())
                    .build();
        }

        AdminSystemSettingsEntity savedSetting = systemSettingsRepo.save(setting);
        logAuditAction("UPDATE_SETTING", "AdminSystemSettingsEntity", savedSetting.getId(), adminId, "Updated setting: " + request.getSettingKey());

        return SystemSettingsResponse.builder()
                .id(savedSetting.getId())
                .settingKey(savedSetting.getSettingKey())
                .settingValue(savedSetting.getSettingValue())
                .description(savedSetting.getDescription())
                .updatedAt(savedSetting.getUpdatedAt())
                .build();
    }

    @Override
    public List<SystemSettingsResponse> getAllSystemSettings() {
        return systemSettingsRepo.findAll().stream()
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
        Page<AdminAuditLogEntity> logPage = auditLogRepo.searchLogs(search, PageRequest.of(page, size, Sort.by("timestamp").descending()));
        List<AuditLogResponse> content = logPage.getContent().stream()
                .map(log -> new AuditLogResponse(
                        log.getId(),
                        log.getAction(),
                        log.getEntityName(),
                        log.getEntityId(),
                        log.getPerformedBy(),
                        log.getDetails(),
                        log.getTimestamp()
                )).toList();
        return new PageResponse<>(content, logPage.getNumber(), logPage.getSize(), logPage.getTotalElements(), logPage.getTotalPages(), logPage.isLast());
    }

    private void logAuditAction(String action, String entityName, UUID entityId, UUID adminId, String details) {
        AdminAuditLogEntity auditLog = AdminAuditLogEntity.builder()
                .action(action)
                .entityName(entityName)
                .entityId(entityId)
                .performedBy(adminId)
                .details(details)
                .build();
        auditLogRepo.save(auditLog);
    }
}
