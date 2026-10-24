package org.proj.service.impl;

import org.proj.dto.MedicalRecordRequest;
import org.proj.dto.MedicalRecordResponse;
import org.proj.entity.AppointmentEntity;
import org.proj.entity.AppointmentEntity.AppointmentStatus;
import org.proj.entity.DoctorEntity;
import org.proj.entity.MedicalRecordEntity;
import org.proj.entity.PatientEntity;
import org.proj.entity.UserEntity;
import org.proj.entity.NotificationEntity.NotificationType;
import org.proj.mapper.MedicalRecordMapper;
import org.proj.repository.MedicalRecordRepo;
import org.proj.security.SecurityUtils;
import org.proj.service.AppointmentService;
import org.proj.service.DoctorService;
import org.proj.service.MedicalRecordService;
import org.proj.service.NotificationService;
import org.proj.service.PatientService;
import org.proj.service.TenantContextService;

import lombok.RequiredArgsConstructor;

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
public class MedicalRecordServiceImpl implements MedicalRecordService {

    private final MedicalRecordRepo medicalRecordRepo;
    private final PatientService patientService;
    private final DoctorService doctorService;
    private final AppointmentService appointmentService;
    private final MedicalRecordMapper medicalRecordMapper;
    private final NotificationService notificationService;
    private final TenantContextService tenantContextService;
    private final org.proj.service.PatientConsentService patientConsentService;

    @Override
    @Transactional
    public MedicalRecordResponse createMedicalRecord(MedicalRecordRequest request) {
        try {
            MedicalRecordRequest validReq = Optional.ofNullable(request)
                    .orElseThrow(() -> new IllegalArgumentException("Medical record request is required"));

            UUID hospitalId = requireCurrentHospital();

            PatientEntity patient = Optional.ofNullable(validReq.getPatientId())
                    .map(patientService::findPatientById)
                    .orElseThrow(() -> new IllegalArgumentException("Patient not found"));

            validatePatientHospital(patient, hospitalId);

            DoctorEntity doctor = Optional.ofNullable(validReq.getDoctorId())
                    .map(doctorService::findDoctorById)
                    .orElseThrow(() -> new IllegalArgumentException("Doctor not found"));

            validateDoctorHospital(doctor, hospitalId);

            UserEntity currentUser = requireCurrentUser();
            String role = getRole(currentUser);

            if ("DOCTOR".equals(role)) {
                DoctorEntity currentDoctor = doctorService.findDoctorEntityByAccountId(currentUser.getId())
                        .orElseThrow(() -> new AccessDeniedException("Doctor profile not found"));

                if (!currentDoctor.getId().equals(doctor.getId())) {
                    throw new AccessDeniedException("You are not authorized to create records for another doctor");
                }

                validateDoctorHospital(currentDoctor, hospitalId);
            }

            AppointmentEntity appointment = null;
            if (validReq.getAppointmentId() != null) {
                AppointmentEntity appt = appointmentService.findAppointmentById(validReq.getAppointmentId());
                if (appt == null) {
                    throw new IllegalArgumentException("Appointment not found");
                }

                validateAppointmentHospital(appt, hospitalId);

                if (appt.getPatient() == null || !patient.getId().equals(appt.getPatient().getId())) {
                    throw new IllegalArgumentException("Appointment does not belong to the specified patient");
                }

                if (appt.getDoctor() == null || !doctor.getId().equals(appt.getDoctor().getId())) {
                    throw new IllegalArgumentException("Appointment does not belong to the specified doctor");
                }

                if (appt.getAppointmentStatus() != AppointmentStatus.IN_PROGRESS) {
                    throw new IllegalArgumentException("Medical record can only be created during an active consultation");
                }

                appointment = appt;
            }

            MedicalRecordEntity entity = medicalRecordMapper.toEntity(validReq);
            entity.setPatient(patient);
            entity.setDoctor(doctor);
            entity.setAppointment(appointment);

            MedicalRecordEntity saved = medicalRecordRepo.save(entity);

            if (patient != null && patient.getAccount() != null) {
                UserEntity docAcc = doctor != null ? doctor.getAccount() : null;
                String docFirstName = docAcc != null && docAcc.getFirstName() != null ? docAcc.getFirstName() : "";
                String docLastName = docAcc != null && docAcc.getLastName() != null ? docAcc.getLastName() : "";

                notificationService.createNotification(
                        patient.getAccount(),
                        "Medical Record Available",
                        ("A new medical record has been created for you by Dr. " + docFirstName + " " + docLastName).trim() + ".",
                        NotificationType.MEDICAL_RECORD_CREATED
                );
            }

            return medicalRecordMapper.toResponse(saved);
        } catch (AccessDeniedException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            String msg = e.getMessage() != null && !e.getMessage().isBlank()
                    ? e.getMessage()
                    : "Unable to create medical record";
            throw new RuntimeException(msg, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public MedicalRecordResponse getMedicalRecordById(UUID id) {
        try {
            UUID validId = Optional.ofNullable(id)
                    .orElseThrow(() -> new IllegalArgumentException("Medical record ID is required"));
            UUID hospitalId = requireCurrentHospital();
            UserEntity currentUser = requireCurrentUser();

            MedicalRecordEntity record = findTenantScopedRecord(validId, hospitalId);
            verifyViewAccess(record, currentUser, hospitalId);

            return medicalRecordMapper.toResponse(record);
        } catch (AccessDeniedException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            String msg = e.getMessage() != null && !e.getMessage().isBlank()
                    ? e.getMessage()
                    : "Unable to retrieve medical record";
            throw new RuntimeException(msg, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public MedicalRecordEntity findMedicalRecordEntityById(UUID id) {
        try {
            UUID validId = Optional.ofNullable(id)
                    .orElseThrow(() -> new IllegalArgumentException("Medical record ID is required"));
            UUID hospitalId = requireCurrentHospital();
            return findTenantScopedRecord(validId, hospitalId);
        } catch (AccessDeniedException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            String msg = e.getMessage() != null && !e.getMessage().isBlank()
                    ? e.getMessage()
                    : "Unable to find medical record entity";
            throw new RuntimeException(msg, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<MedicalRecordResponse> getPatientMedicalRecords(UUID patientId) {
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
                UserEntity patientAcc = patient != null ? patient.getAccount() : null;
                UUID patientUserId = patientAcc != null ? patientAcc.getId() : null;
                if (patientUserId == null || currentUser == null || !patientUserId.equals(currentUser.getId())) {
                    throw new AccessDeniedException("You are not authorized to view this patient's medical records");
                }
            }

            List<MedicalRecordEntity> records = medicalRecordRepo.findByPatientIdAndPatientHospitalId(
                    validPatientId,
                    hospitalId
            );

            if ("DOCTOR".equals(role)) {
                DoctorEntity currentDoctor = doctorService.findDoctorEntityByAccountId(currentUser.getId())
                        .orElseThrow(() -> new AccessDeniedException("Doctor profile not found"));

                validateDoctorHospital(currentDoctor, hospitalId);

                patientConsentService.verifyConsentForDoctor(
                        validPatientId,
                        currentDoctor.getId(),
                        org.proj.entity.PatientConsentEntity.ConsentType.MEDICAL_RECORD_SHARING,
                        "MEDICAL_RECORDS"
                );

                records = records.stream()
                        .filter(record -> record.getDoctor() != null && currentDoctor.getId().equals(record.getDoctor().getId()))
                        .collect(Collectors.toList());
            }

            return records.stream()
                    .map(medicalRecordMapper::toResponse)
                    .collect(Collectors.toList());
        } catch (AccessDeniedException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            String msg = e.getMessage() != null && !e.getMessage().isBlank()
                    ? e.getMessage()
                    : "Unable to retrieve patient medical records";
            throw new RuntimeException(msg, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<MedicalRecordResponse> getDoctorMedicalRecords(UUID doctorId) {
        try {
            UUID validDoctorId = Optional.ofNullable(doctorId)
                    .orElseThrow(() -> new IllegalArgumentException("Doctor ID is required"));
            UUID hospitalId = requireCurrentHospital();
            UserEntity currentUser = requireCurrentUser();

            DoctorEntity doctor = Optional.ofNullable(doctorService.findDoctorById(validDoctorId))
                    .orElseThrow(() -> new IllegalArgumentException("Doctor not found"));

            validateDoctorHospital(doctor, hospitalId);

            String role = getRole(currentUser);

            if ("DOCTOR".equals(role)) {
                UserEntity docAcc = doctor != null ? doctor.getAccount() : null;
                UUID docUserId = docAcc != null ? docAcc.getId() : null;
                if (docUserId == null || currentUser == null || !docUserId.equals(currentUser.getId())) {
                    throw new AccessDeniedException("You are not authorized to view this doctor's medical records");
                }
            }

            return medicalRecordRepo.findByDoctorIdAndDoctorHospitalId(validDoctorId, hospitalId).stream()
                    .map(medicalRecordMapper::toResponse)
                    .collect(Collectors.toList());
        } catch (AccessDeniedException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            String msg = e.getMessage() != null && !e.getMessage().isBlank()
                    ? e.getMessage()
                    : "Unable to retrieve doctor medical records";
            throw new RuntimeException(msg, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public MedicalRecordResponse getMedicalRecordByAppointment(UUID appointmentId) {
        try {
            UUID validApptId = Optional.ofNullable(appointmentId)
                    .orElseThrow(() -> new IllegalArgumentException("Appointment ID is required"));
            UUID hospitalId = requireCurrentHospital();

            AppointmentEntity appointment = Optional.ofNullable(appointmentService.findAppointmentById(validApptId))
                    .orElseThrow(() -> new IllegalArgumentException("Appointment not found"));

            validateAppointmentHospital(appointment, hospitalId);

            return medicalRecordRepo.findByAppointmentIdAndAppointmentHospitalId(validApptId, hospitalId).stream()
                    .findFirst()
                    .map(record -> {
                        Optional.ofNullable(record.getPatient())
                                .filter(p -> record.getDoctor() != null)
                                .orElseThrow(() -> new IllegalArgumentException("Invalid medical record relationship"));

                        validatePatientHospital(record.getPatient(), hospitalId);
                        validateDoctorHospital(record.getDoctor(), hospitalId);

                        return medicalRecordMapper.toResponse(record);
                    })
                    .orElse(null);
        } catch (AccessDeniedException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            String msg = e.getMessage() != null && !e.getMessage().isBlank()
                    ? e.getMessage()
                    : "Unable to retrieve medical record by appointment";
            throw new RuntimeException(msg, e);
        }
    }

    private MedicalRecordEntity findTenantScopedRecord(UUID id, UUID hospitalId) {
        return medicalRecordRepo.findByIdAndPatientHospitalId(id, hospitalId)
                .orElseThrow(() -> new IllegalArgumentException("Medical Record not found"));
    }

    private void verifyViewAccess(
            MedicalRecordEntity record,
            UserEntity currentUser,
            UUID hospitalId
    ) {
        Optional.ofNullable(record)
                .orElseThrow(() -> new IllegalArgumentException("Medical Record not found"));

        Optional.ofNullable(record.getPatient())
                .filter(p -> record.getDoctor() != null)
                .orElseThrow(() -> new AccessDeniedException("Invalid medical record ownership"));

        validatePatientHospital(record.getPatient(), hospitalId);
        validateDoctorHospital(record.getDoctor(), hospitalId);

        String role = getRole(currentUser);

        if ("ADMIN".equals(role) || "RECEPTIONIST".equals(role)) {
            return;
        }

        if ("PATIENT".equals(role)) {
            UserEntity patientAcc = record.getPatient() != null ? record.getPatient().getAccount() : null;
            UUID patientUserId = patientAcc != null ? patientAcc.getId() : null;
            if (patientUserId == null || currentUser == null || !patientUserId.equals(currentUser.getId())) {
                throw new AccessDeniedException("You can only access your own medical records");
            }
        }

        if ("DOCTOR".equals(role)) {
            DoctorEntity currentDoctor = doctorService.findDoctorEntityByAccountId(currentUser.getId())
                    .orElseThrow(() -> new AccessDeniedException("Doctor profile not found"));

            validateDoctorHospital(currentDoctor, hospitalId);

            UUID recordDoctorId = record.getDoctor() != null ? record.getDoctor().getId() : null;
            if (recordDoctorId == null || !currentDoctor.getId().equals(recordDoctorId)) {
                throw new AccessDeniedException("You can only access medical records for your own consultations");
            }

            if (record.getPatient() != null) {
                patientConsentService.verifyConsentForDoctor(
                        record.getPatient().getId(),
                        currentDoctor.getId(),
                        org.proj.entity.PatientConsentEntity.ConsentType.MEDICAL_RECORD_SHARING,
                        "MEDICAL_RECORDS"
                );
            }
        }
    }

    private UUID requireCurrentHospital() {
        UUID hospitalId = tenantContextService != null ? tenantContextService.getCurrentUserHospitalId() : null;
        if (hospitalId == null) throw new AccessDeniedException("User is not associated with a hospital");
        return hospitalId;
    }

    private UserEntity requireCurrentUser() {
        UserEntity currentUser = SecurityUtils.getCurrentUser();
        if (currentUser == null) throw new AccessDeniedException("Authentication required");
        return currentUser;
    }

    private String getRole(UserEntity user) {
        String roleName = user != null && user.getRole() != null ? user.getRole().getRoleName() : null;
        if (roleName == null || roleName.isBlank()) throw new AccessDeniedException("User role not found");
        return roleName.trim().toUpperCase(Locale.ROOT);
    }

    private void validatePatientHospital(PatientEntity patient, UUID hospitalId) {
        UUID patientHospitalId = patient != null && patient.getHospital() != null ? patient.getHospital().getId() : null;
        if (patientHospitalId == null || !patientHospitalId.equals(hospitalId)) {
            throw new AccessDeniedException("You are not authorized to access patient data from another hospital");
        }
    }

    private void validateDoctorHospital(DoctorEntity doctor, UUID hospitalId) {
        UUID doctorHospitalId = doctor != null && doctor.getHospital() != null ? doctor.getHospital().getId() : null;
        if (doctorHospitalId == null || !doctorHospitalId.equals(hospitalId)) {
            throw new AccessDeniedException("You are not authorized to access doctor data from another hospital");
        }
    }

    private void validateAppointmentHospital(AppointmentEntity appointment, UUID hospitalId) {
        UUID apptHospitalId = appointment != null && appointment.getHospital() != null ? appointment.getHospital().getId() : null;
        if (apptHospitalId == null || !apptHospitalId.equals(hospitalId)) {
            throw new AccessDeniedException("You are not authorized to access an appointment from another hospital");
        }
    }
}
