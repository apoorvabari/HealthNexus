package org.proj.service.impl;

import lombok.RequiredArgsConstructor;
import org.proj.dto.MedicalReportRequest;
import org.proj.dto.MedicalReportResponse;
import org.proj.entity.DoctorEntity;
import org.proj.entity.MedicalReportEntity;
import org.proj.entity.NotificationEntity.NotificationType;
import org.proj.entity.PatientEntity;
import org.proj.entity.UserEntity;
import org.proj.repository.MedicalReportRepo;
import org.proj.security.SecurityUtils;
import org.proj.service.AppointmentService;
import org.proj.service.DoctorService;
import org.proj.service.MedicalReportService;
import org.proj.service.NotificationService;
import org.proj.service.PatientService;
import org.proj.service.TenantContextService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MedicalReportServiceImpl implements MedicalReportService {

    private final MedicalReportRepo medicalReportRepo;
    private final PatientService patientService;
    private final DoctorService doctorService;
    private final NotificationService notificationService;
    private final AppointmentService appointmentService;
    private final TenantContextService tenantContextService;

    @Override
    @Transactional
    public MedicalReportResponse uploadReport(MedicalReportRequest request) {
        try {
            MedicalReportRequest validReq = Optional.ofNullable(request)
                    .orElseThrow(() -> new IllegalArgumentException("Medical report request is required"));

            PatientEntity patient = Optional.ofNullable(validReq.getPatientId())
                    .map(patientService::findPatientById)
                    .orElseThrow(() -> new IllegalArgumentException("Patient not found"));

            UserEntity currentUser = requireCurrentUser();
            String role = getRole(currentUser);

            DoctorEntity doctor = null;
            if ("ADMIN".equals(role)) {
                validatePatientHospital(patient, requireCurrentHospital());
            } else if ("DOCTOR".equals(role)) {
                UUID currentHospitalId = requireCurrentHospital();
                DoctorEntity currentDoc = doctorService.findDoctorEntityByAccountId(currentUser.getId())
                        .orElseThrow(() -> new AccessDeniedException("Doctor profile not found"));

                validateDoctorHospital(currentDoc, currentHospitalId);
                validatePatientHospital(patient, currentHospitalId);

                if (!appointmentService.existsByDoctorIdAndPatientIdAndHospitalId(currentDoc.getId(), patient.getId(), currentHospitalId)) {
                    throw new AccessDeniedException("Doctor is not authorized to access this patient");
                }
                doctor = currentDoc;
            } else {
                throw new AccessDeniedException("Only doctors or admins can upload medical reports");
            }

            MedicalReportEntity.ReportType reportType = Optional.ofNullable(validReq.getReportType())
                    .map(typeStr -> {
                        try {
                            return MedicalReportEntity.ReportType.valueOf(typeStr.toUpperCase());
                        } catch (IllegalArgumentException e) {
                            return MedicalReportEntity.ReportType.OTHER;
                        }
                    })
                    .orElse(MedicalReportEntity.ReportType.OTHER);

            MedicalReportEntity report = MedicalReportEntity.builder()
                    .patient(patient)
                    .doctor(doctor)
                    .reportName(validReq.getReportName())
                    .reportType(reportType)
                    .fileData(validReq.getFileData())
                    .build();

            MedicalReportEntity savedReport = medicalReportRepo.save(report);

            Optional.ofNullable(patient.getAccount()).ifPresent(acc ->
                    notificationService.createNotification(
                            acc,
                            "Report Available",
                            "A new medical report (" + savedReport.getReportName() + ") is available.",
                            NotificationType.REPORT_AVAILABLE
                    )
            );

            return mapToResponse(savedReport, true);
        } catch (AccessDeniedException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            String msg = e.getMessage() != null && !e.getMessage().isBlank()
                    ? e.getMessage()
                    : "Unable to upload medical report";
            throw new RuntimeException(msg, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public MedicalReportResponse getReportById(UUID id) {
        try {
            UUID validId = Optional.ofNullable(id)
                    .orElseThrow(() -> new IllegalArgumentException("Report ID is required"));

            UUID hospitalId = requireCurrentHospital();
            UserEntity currentUser = requireCurrentUser();

            MedicalReportEntity report = findTenantScopedReport(validId, hospitalId);
            verifyViewAccess(report, currentUser, hospitalId);

            return mapToResponse(report, true);
        } catch (AccessDeniedException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            String msg = e.getMessage() != null && !e.getMessage().isBlank()
                    ? e.getMessage()
                    : "Unable to retrieve medical report";
            throw new RuntimeException(msg, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<MedicalReportResponse> getReportsByPatientId(UUID patientId) {
        try {
            UUID validPatientId = Optional.ofNullable(patientId)
                    .orElseThrow(() -> new IllegalArgumentException("Patient ID is required"));

            UUID hospitalId = requireCurrentHospital();
            UserEntity currentUser = requireCurrentUser();

            PatientEntity patient = Optional.ofNullable(patientService.findPatientById(validPatientId))
                    .orElseThrow(() -> new IllegalArgumentException("Patient not found"));

            validatePatientHospital(patient, hospitalId);

            String role = getRole(currentUser);

            if ("PATIENT".equals(role)) {
                UserEntity patientAcc = patient.getAccount();
                UUID patientUserId = patientAcc != null ? patientAcc.getId() : null;
                if (patientUserId == null || currentUser == null || !patientUserId.equals(currentUser.getId())) {
                    throw new AccessDeniedException("You are not authorized to view this patient's medical reports");
                }
            }

            if ("DOCTOR".equals(role)) {
                DoctorEntity currentDoctor = doctorService.findDoctorEntityByAccountId(currentUser.getId())
                        .orElseThrow(() -> new AccessDeniedException("Doctor profile not found"));

                validateDoctorHospital(currentDoctor, hospitalId);

                UUID patId = (patient != null) ? patient.getId() : null;
                if (patId == null || !appointmentService.existsByDoctorIdAndPatientIdAndHospitalId(
                        currentDoctor.getId(),
                        patId,
                        hospitalId
                )) {
                    throw new AccessDeniedException("Doctor is not authorized to access this patient");
                }
            }

            Optional.of(role)
                    .filter(r -> "ADMIN".equals(r) || "RECEPTIONIST".equals(r) || "DOCTOR".equals(r) || "PATIENT".equals(r))
                    .orElseThrow(() -> new AccessDeniedException("You are not authorized to view these reports"));

            return medicalReportRepo.findByPatientIdAndPatientHospitalIdOrderByCreatedAtDesc(validPatientId, hospitalId)
                    .stream()
                    .map(report -> mapToResponse(report, false))
                    .collect(Collectors.toList());
        } catch (AccessDeniedException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            String msg = e.getMessage() != null && !e.getMessage().isBlank()
                    ? e.getMessage()
                    : "Unable to retrieve medical reports for patient";
            throw new RuntimeException(msg, e);
        }
    }

    @Override
    @Transactional
    public void deleteReport(UUID id) {
        try {
            UUID validId = Optional.ofNullable(id)
                    .orElseThrow(() -> new IllegalArgumentException("Report ID is required"));

            UUID hospitalId = requireCurrentHospital();
            UserEntity currentUser = requireCurrentUser();

            MedicalReportEntity report = findTenantScopedReport(validId, hospitalId);
            String role = getRole(currentUser);

            Optional.of(role)
                    .filter("ADMIN"::equals)
                    .ifPresent(r -> validatePatientHospital(report.getPatient(), hospitalId));

            if ("DOCTOR".equals(role)) {
                DoctorEntity currentDoctor = doctorService.findDoctorEntityByAccountId(currentUser.getId())
                        .orElseThrow(() -> new AccessDeniedException("Doctor profile not found"));

                validateDoctorHospital(currentDoctor, hospitalId);

                UUID reportDocId = report.getDoctor() != null ? report.getDoctor().getId() : null;
                if (reportDocId == null || !currentDoctor.getId().equals(reportDocId)) {
                    throw new AccessDeniedException("You are not authorized to delete this report");
                }
            }

            Optional.of(role)
                    .filter(r -> "ADMIN".equals(r) || "DOCTOR".equals(r))
                    .orElseThrow(() -> new AccessDeniedException("You are not authorized to delete this report"));

            medicalReportRepo.delete(report);
        } catch (AccessDeniedException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            String msg = e.getMessage() != null && !e.getMessage().isBlank()
                    ? e.getMessage()
                    : "Unable to delete medical report";
            throw new RuntimeException(msg, e);
        }
    }

    private MedicalReportEntity findTenantScopedReport(UUID id, UUID hospitalId) {
        return medicalReportRepo.findByIdAndPatientHospitalId(id, hospitalId)
                .orElseGet(() -> {
                    MedicalReportEntity existingReport = medicalReportRepo.findById(id)
                            .orElseThrow(() -> new IllegalArgumentException("Report not found"));

                    UUID patientHospitalId = (existingReport.getPatient() != null && existingReport.getPatient().getHospital() != null)
                            ? existingReport.getPatient().getHospital().getId()
                            : null;

                    if (patientHospitalId == null || !patientHospitalId.equals(hospitalId)) {
                        throw new AccessDeniedException("You are not authorized to access this report");
                    }

                    return existingReport;
                });
    }

    private void verifyViewAccess(MedicalReportEntity report, UserEntity currentUser, UUID hospitalId) {
        Optional.ofNullable(report.getPatient())
                .orElseThrow(() -> new AccessDeniedException("Invalid medical report ownership"));

        validatePatientHospital(report.getPatient(), hospitalId);
        String role = getRole(currentUser);

        if ("ADMIN".equals(role) || "RECEPTIONIST".equals(role)) {
            return;
        }

        if ("PATIENT".equals(role)) {
            UserEntity patientAcc = (report.getPatient() != null) ? report.getPatient().getAccount() : null;
            UUID patientUserId = (patientAcc != null) ? patientAcc.getId() : null;
            if (patientUserId == null || currentUser == null || !patientUserId.equals(currentUser.getId())) {
                throw new AccessDeniedException("You can only access your own medical reports");
            }
            return;
        }

        if ("DOCTOR".equals(role)) {
            DoctorEntity currentDoctor = doctorService.findDoctorEntityByAccountId(currentUser.getId())
                    .orElseThrow(() -> new AccessDeniedException("Doctor profile not found"));

            validateDoctorHospital(currentDoctor, hospitalId);

            if (appointmentService.existsByDoctorIdAndPatientIdAndHospitalId(
                    currentDoctor.getId(),
                    report.getPatient().getId(),
                    hospitalId
            )) {
                return;
            }
        }

        throw new AccessDeniedException("You are not authorized to view this report");
    }

    private UUID requireCurrentHospital() {
        return Optional.ofNullable(tenantContextService.getCurrentUserHospitalId())
                .orElseThrow(() -> new AccessDeniedException("User is not associated with a hospital"));
    }

    private UserEntity requireCurrentUser() {
        UserEntity currentUser = SecurityUtils.getCurrentUser();
        if (currentUser == null) throw new AccessDeniedException("Authentication required");
        return currentUser;
    }

    private String getRole(UserEntity user) {
        String roleName = (user != null && user.getRole() != null) ? user.getRole().getRoleName() : null;
        if (roleName == null || roleName.isBlank()) throw new AccessDeniedException("User role not found");
        return roleName.trim().toUpperCase(Locale.ROOT);
    }

    private void validatePatientHospital(PatientEntity patient, UUID hospitalId) {
        UUID patientHospitalId = (patient != null && patient.getHospital() != null) ? patient.getHospital().getId() : null;
        if (patientHospitalId == null || !patientHospitalId.equals(hospitalId)) {
            throw new AccessDeniedException("You are not authorized to access patient data from another hospital");
        }
    }

    private void validateDoctorHospital(DoctorEntity doctor, UUID hospitalId) {
        UUID doctorHospitalId = (doctor != null && doctor.getHospital() != null) ? doctor.getHospital().getId() : null;
        if (doctorHospitalId == null || !doctorHospitalId.equals(hospitalId)) {
            throw new AccessDeniedException("You are not authorized to access doctor data from another hospital");
        }
    }

    private MedicalReportResponse mapToResponse(MedicalReportEntity entity, boolean includeFileData) {
        UserEntity patAcc = (entity.getPatient() != null) ? entity.getPatient().getAccount() : null;
        String patFirst = patAcc != null && patAcc.getFirstName() != null ? patAcc.getFirstName() : "";
        String patLast = patAcc != null && patAcc.getLastName() != null ? patAcc.getLastName() : "";
        String patientName = (patFirst + " " + patLast).trim();

        UserEntity docAcc = (entity.getDoctor() != null) ? entity.getDoctor().getAccount() : null;
        String doctorName = null;
        if (docAcc != null) {
            String docFirst = docAcc.getFirstName() != null ? docAcc.getFirstName() : "";
            String docLast = docAcc.getLastName() != null ? docAcc.getLastName() : "";
            doctorName = (docFirst + " " + docLast).trim();
        }

        return MedicalReportResponse.builder()
                .id(entity.getId())
                .patientId(entity.getPatient() != null ? entity.getPatient().getId() : null)
                .patientName(patientName)
                .doctorId(entity.getDoctor() != null ? entity.getDoctor().getId() : null)
                .doctorName(doctorName)
                .reportName(entity.getReportName())
                .reportType(entity.getReportType() != null ? entity.getReportType().name() : null)
                .fileData(includeFileData ? entity.getFileData() : null)
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
