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

        PatientEntity patient = patientService.findPatientById(request.getPatientId());

        if (patient == null) {
            throw new IllegalArgumentException("Patient not found");
        }

        UserEntity currentUser = requireCurrentUser();
        String role = getRole(currentUser);

        DoctorEntity doctor = null;

        if ("ADMIN".equals(role)) {

            UUID currentHospitalId = requireCurrentHospital();

            validatePatientHospital(patient, currentHospitalId);

        } else if ("DOCTOR".equals(role)) {

            UUID currentHospitalId = requireCurrentHospital();

            doctor = doctorService
                    .findDoctorEntityByAccountId(currentUser.getId())
                    .orElseThrow(() ->
                            new AccessDeniedException("Doctor profile not found"));

            validateDoctorHospital(doctor, currentHospitalId);
            validatePatientHospital(patient, currentHospitalId);

            boolean hasAppointment =
                    appointmentService.existsByDoctorIdAndPatientIdAndHospitalId(
                            doctor.getId(),
                            patient.getId(),
                            currentHospitalId
                    );

            if (!hasAppointment) {
                throw new AccessDeniedException(
                        "Doctor is not authorized to access this patient"
                );
            }

        } else {

            throw new AccessDeniedException(
                    "Only doctors or admins can upload medical reports"
            );
        }

        MedicalReportEntity.ReportType reportType;

        try {
            reportType = MedicalReportEntity.ReportType
                    .valueOf(request.getReportType().toUpperCase());

        } catch (IllegalArgumentException | NullPointerException e) {

            reportType = MedicalReportEntity.ReportType.OTHER;
        }

        MedicalReportEntity report =
                MedicalReportEntity.builder()
                        .patient(patient)
                        .doctor(doctor)
                        .reportName(request.getReportName())
                        .reportType(reportType)
                        .fileData(request.getFileData())
                        .build();

        MedicalReportEntity savedReport =
                medicalReportRepo.save(report);

        notificationService.createNotification(
                patient.getAccount(),
                "Report Available",
                "A new medical report ("
                        + savedReport.getReportName()
                        + ") is available.",
                NotificationType.REPORT_AVAILABLE
        );

        return mapToResponse(savedReport, true);
    }

    @Override
    @Transactional(readOnly = true)
    public MedicalReportResponse getReportById(UUID id) {

        if (id == null) {
            throw new IllegalArgumentException("Report ID is required");
        }

        UUID hospitalId = requireCurrentHospital();
        UserEntity currentUser = requireCurrentUser();

        MedicalReportEntity report =
                findTenantScopedReport(id, hospitalId);

        verifyViewAccess(
                report,
                currentUser,
                hospitalId
        );

        return mapToResponse(report, true);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MedicalReportResponse> getReportsByPatientId(
            UUID patientId) {

        if (patientId == null) {
            throw new IllegalArgumentException("Patient ID is required");
        }

        UUID hospitalId = requireCurrentHospital();
        UserEntity currentUser = requireCurrentUser();

        PatientEntity patient =
                patientService.findPatientById(patientId);

        if (patient == null) {
            throw new IllegalArgumentException("Patient not found");
        }

        validatePatientHospital(patient, hospitalId);

        String role = getRole(currentUser);

        if ("PATIENT".equals(role)) {

            if (patient.getAccount() == null
                    || !patient.getAccount()
                    .getId()
                    .equals(currentUser.getId())) {

                throw new AccessDeniedException(
                        "You are not authorized to view this patient's medical reports"
                );
            }

        } else if ("DOCTOR".equals(role)) {

            DoctorEntity currentDoctor =
                    doctorService
                            .findDoctorEntityByAccountId(
                                    currentUser.getId()
                            )
                            .orElseThrow(() ->
                                    new AccessDeniedException(
                                            "Doctor profile not found"
                                    ));

            validateDoctorHospital(
                    currentDoctor,
                    hospitalId
            );

            boolean hasAppointment =
                    appointmentService
                            .existsByDoctorIdAndPatientIdAndHospitalId(
                                    currentDoctor.getId(),
                                    patient.getId(),
                                    hospitalId
                            );

            if (!hasAppointment) {
                throw new AccessDeniedException(
                        "Doctor is not authorized to access this patient"
                );
            }

        } else if (!"ADMIN".equals(role)
                && !"RECEPTIONIST".equals(role)) {

            throw new AccessDeniedException(
                    "You are not authorized to view these reports"
            );
        }

        return medicalReportRepo
                .findByPatientIdAndPatientHospitalIdOrderByCreatedAtDesc(
                        patientId,
                        hospitalId
                )
                .stream()
                .map(report -> mapToResponse(report, false))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteReport(UUID id) {

        if (id == null) {
            throw new IllegalArgumentException("Report ID is required");
        }

        UUID hospitalId = requireCurrentHospital();
        UserEntity currentUser = requireCurrentUser();

        MedicalReportEntity report =
                findTenantScopedReport(id, hospitalId);

        String role = getRole(currentUser);

        if ("ADMIN".equals(role)) {

            validatePatientHospital(
                    report.getPatient(),
                    hospitalId
            );

        } else if ("DOCTOR".equals(role)) {

            DoctorEntity currentDoctor =
                    doctorService
                            .findDoctorEntityByAccountId(
                                    currentUser.getId()
                            )
                            .orElseThrow(() ->
                                    new AccessDeniedException(
                                            "Doctor profile not found"
                                    ));

            validateDoctorHospital(
                    currentDoctor,
                    hospitalId
            );

            if (report.getDoctor() == null
                    || !report.getDoctor()
                    .getId()
                    .equals(currentDoctor.getId())) {

                throw new AccessDeniedException(
                        "You are not authorized to delete this report"
                );
            }

        } else {

            throw new AccessDeniedException(
                    "You are not authorized to delete this report"
            );
        }

        medicalReportRepo.delete(report);
    }

    /**
     * First use tenant-scoped lookup.
     *
     * If the report exists but belongs to another hospital,
     * return 403 instead of leaking it as 404.
     *
     * If it does not exist at all, return 404.
     */
    private MedicalReportEntity findTenantScopedReport(
            UUID id,
            UUID hospitalId) {

        return medicalReportRepo
                .findByIdAndPatientHospitalId(id, hospitalId)
                .orElseGet(() -> {

                    MedicalReportEntity existingReport =
                            medicalReportRepo.findById(id)
                                    .orElse(null);

                    if (existingReport == null) {
                        throw new IllegalArgumentException(
                                "Report not found"
                        );
                    }

                    if (existingReport.getPatient() == null
                            || existingReport.getPatient().getHospital() == null
                            || existingReport.getPatient()
                            .getHospital()
                            .getId() == null) {

                        throw new AccessDeniedException(
                                "Invalid medical report tenant"
                        );
                    }

                    if (!hospitalId.equals(
                            existingReport
                                    .getPatient()
                                    .getHospital()
                                    .getId())) {

                        throw new AccessDeniedException(
                                "You are not authorized to access this report"
                        );
                    }

                    return existingReport;
                });
    }

    private void verifyViewAccess(
            MedicalReportEntity report,
            UserEntity currentUser,
            UUID hospitalId) {

        if (report.getPatient() == null) {
            throw new AccessDeniedException(
                    "Invalid medical report ownership"
            );
        }

        validatePatientHospital(
                report.getPatient(),
                hospitalId
        );

        String role = getRole(currentUser);

        if ("ADMIN".equals(role)
                || "RECEPTIONIST".equals(role)) {

            return;
        }

        if ("PATIENT".equals(role)) {

            if (report.getPatient().getAccount() == null
                    || !report.getPatient()
                    .getAccount()
                    .getId()
                    .equals(currentUser.getId())) {

                throw new AccessDeniedException(
                        "You can only access your own medical reports"
                );
            }

            return;
        }

        if ("DOCTOR".equals(role)) {

            DoctorEntity currentDoctor =
                    doctorService
                            .findDoctorEntityByAccountId(
                                    currentUser.getId()
                            )
                            .orElseThrow(() ->
                                    new AccessDeniedException(
                                            "Doctor profile not found"
                                    ));

            validateDoctorHospital(
                    currentDoctor,
                    hospitalId
            );

            boolean hasAppointment =
                    appointmentService
                            .existsByDoctorIdAndPatientIdAndHospitalId(
                                    currentDoctor.getId(),
                                    report.getPatient().getId(),
                                    hospitalId
                            );

            if (hasAppointment) {
                return;
            }
        }

        throw new AccessDeniedException(
                "You are not authorized to view this report"
        );
    }

    private UUID requireCurrentHospital() {

        UUID hospitalId =
                tenantContextService
                        .getCurrentUserHospitalId();

        if (hospitalId == null) {
            throw new AccessDeniedException(
                    "User is not associated with a hospital"
            );
        }

        return hospitalId;
    }

    private UserEntity requireCurrentUser() {

        UserEntity currentUser =
                SecurityUtils.getCurrentUser();

        if (currentUser == null) {
            throw new AccessDeniedException(
                    "Authentication required"
            );
        }

        return currentUser;
    }

    private String getRole(UserEntity user) {

        if (user.getRole() == null
                || user.getRole().getRoleName() == null) {

            throw new AccessDeniedException(
                    "User role not found"
            );
        }

        return user.getRole()
                .getRoleName()
                .toUpperCase();
    }

    private void validatePatientHospital(
            PatientEntity patient,
            UUID hospitalId) {

        if (patient == null
                || patient.getHospital() == null
                || patient.getHospital().getId() == null
                || !hospitalId.equals(
                patient.getHospital().getId())) {

            throw new AccessDeniedException(
                    "You are not authorized to access patient data from another hospital"
            );
        }
    }

    private void validateDoctorHospital(
            DoctorEntity doctor,
            UUID hospitalId) {

        if (doctor == null
                || doctor.getHospital() == null
                || doctor.getHospital().getId() == null
                || !hospitalId.equals(
                doctor.getHospital().getId())) {

            throw new AccessDeniedException(
                    "You are not authorized to access doctor data from another hospital"
            );
        }
    }

    private MedicalReportResponse mapToResponse(
            MedicalReportEntity entity,
            boolean includeFileData) {

        String patientName = "";

        if (entity.getPatient() != null
                && entity.getPatient().getAccount() != null) {

            patientName =
                    (
                            entity.getPatient()
                                    .getAccount()
                                    .getFirstName()
                                    + " "
                                    + entity.getPatient()
                                    .getAccount()
                                    .getLastName()
                    ).trim();
        }

        String doctorName = null;

        if (entity.getDoctor() != null
                && entity.getDoctor().getAccount() != null) {

            doctorName =
                    (
                            entity.getDoctor()
                                    .getAccount()
                                    .getFirstName()
                                    + " "
                                    + entity.getDoctor()
                                    .getAccount()
                                    .getLastName()
                    ).trim();
        }

        return MedicalReportResponse.builder()
                .id(entity.getId())
                .patientId(
                        entity.getPatient() != null
                                ? entity.getPatient().getId()
                                : null
                )
                .patientName(patientName)
                .doctorId(
                        entity.getDoctor() != null
                                ? entity.getDoctor().getId()
                                : null
                )
                .doctorName(doctorName)
                .reportName(entity.getReportName())
                .reportType(
                        entity.getReportType() != null
                                ? entity.getReportType().name()
                                : null
                )
                .fileData(
                        includeFileData
                                ? entity.getFileData()
                                : null
                )
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
