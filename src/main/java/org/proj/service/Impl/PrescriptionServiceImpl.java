package org.proj.service.impl;

import lombok.RequiredArgsConstructor;
import org.proj.dto.PrescriptionRequest;
import org.proj.dto.PrescriptionResponse;
import org.proj.entity.AppointmentEntity;
import org.proj.entity.DoctorEntity;
import org.proj.entity.MedicalRecordEntity;
import org.proj.entity.PatientEntity;
import org.proj.entity.PrescriptionEntity;
import org.proj.entity.UserEntity;
import org.proj.entity.NotificationEntity.NotificationType;
import org.proj.mapper.PrescriptionMapper;
import org.proj.repository.PrescriptionRepo;
import org.proj.security.SecurityUtils;
import org.proj.service.AppointmentService;
import org.proj.service.DoctorService;
import org.proj.service.MedicalRecordService;
import org.proj.service.NotificationService;
import org.proj.service.PatientService;
import org.proj.service.PrescriptionService;
import org.proj.service.TenantContextService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PrescriptionServiceImpl implements PrescriptionService {

    private final PrescriptionRepo prescriptionRepo;
    private final AppointmentService appointmentService;
    private final MedicalRecordService medicalRecordService;
    private final PatientService patientService;
    private final DoctorService doctorService;
    private final PrescriptionMapper prescriptionMapper;
    private final NotificationService notificationService;
    private final TenantContextService tenantContextService;
    private final org.proj.service.PatientConsentService patientConsentService;

    @Override
    @Transactional
    public PrescriptionResponse createPrescription(PrescriptionRequest request) {
        try {
            PrescriptionRequest validReq = Optional.ofNullable(request)
                    .orElseThrow(() -> new IllegalArgumentException("Appointment ID is required"));

            UUID appointmentId = Optional.ofNullable(validReq.getAppointmentId())
                    .orElseThrow(() -> new IllegalArgumentException("Appointment ID is required"));

            UUID hospitalId = requireHospital();

            AppointmentEntity appointment = Optional.ofNullable(appointmentService.findAppointmentById(appointmentId))
                    .orElseThrow(() -> new IllegalArgumentException("Appointment not found"));

            validateAppointmentTenant(appointment, hospitalId);

            List<PrescriptionEntity> existing = prescriptionRepo.findByAppointmentId(appointmentId);
            if (!existing.isEmpty()) {
                return updatePrescription(existing.get(0).getId(), validReq);
            }

            Optional.ofNullable(appointment.getAppointmentStatus())
                    .filter(status -> status == AppointmentEntity.AppointmentStatus.IN_PROGRESS
                            || status == AppointmentEntity.AppointmentStatus.COMPLETED)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Prescription can only be created during an active or completed consultation"));

            PatientEntity patient = appointment.getPatient();
            DoctorEntity doctor = appointment.getDoctor();

            validateCreateAccess(doctor, hospitalId);

            MedicalRecordEntity medicalRecord = Optional.ofNullable(validReq.getMedicalRecordId())
                    .map(recordId -> {
                        MedicalRecordEntity medRecord = medicalRecordService.findMedicalRecordEntityById(recordId);
                        validateMedicalRecord(medRecord, appointment, hospitalId);
                        return medRecord;
                    })
                    .orElse(null);

            PrescriptionEntity entity = prescriptionMapper.toEntity(validReq);
            entity.setPatient(patient);
            entity.setDoctor(doctor);
            entity.setAppointment(appointment);
            entity.setMedicalRecord(medicalRecord);
            entity.setPrescriptionDate(LocalDate.now());

            PrescriptionEntity saved = prescriptionRepo.save(entity);

            if (patient != null && patient.getAccount() != null) {
                UserEntity docAcc = (doctor != null) ? doctor.getAccount() : null;
                String docFirst = (docAcc != null && docAcc.getFirstName() != null) ? docAcc.getFirstName() : "";
                String docLast = (docAcc != null && docAcc.getLastName() != null) ? docAcc.getLastName() : "";

                notificationService.createNotification(
                        patient.getAccount(),
                        "Prescription Created",
                        ("A new prescription has been created for you by Dr. " + docFirst + " " + docLast).trim() + ".",
                        NotificationType.PRESCRIPTION_CREATED
                );
            }

            return prescriptionMapper.toResponse(saved);
        } catch (AccessDeniedException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            String msg = e.getMessage() != null && !e.getMessage().isBlank()
                    ? e.getMessage()
                    : "Unable to create prescription";
            throw new RuntimeException(msg, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PrescriptionResponse getPrescriptionById(UUID id) {
        try {
            UUID validId = Optional.ofNullable(id)
                    .orElseThrow(() -> new IllegalArgumentException("Prescription ID is required"));

            UUID hospitalId = requireHospital();

            PrescriptionEntity prescription = prescriptionRepo.findByIdAndAppointmentHospitalId(validId, hospitalId)
                    .orElseThrow(() -> new IllegalArgumentException("Prescription not found"));

            validateViewAccess(prescription, hospitalId);

            return prescriptionMapper.toResponse(prescription);
        } catch (AccessDeniedException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            String msg = e.getMessage() != null && !e.getMessage().isBlank()
                    ? e.getMessage()
                    : "Unable to retrieve prescription";
            throw new RuntimeException(msg, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<PrescriptionResponse> getPatientPrescriptions(UUID patientId) {
        try {
            UUID validPatientId = Optional.ofNullable(patientId)
                    .orElseThrow(() -> new IllegalArgumentException("Patient ID is required"));

            UUID hospitalId = requireHospital();

            PatientEntity patient = Optional.ofNullable(patientService.findPatientById(validPatientId))
                    .orElseThrow(() -> new IllegalArgumentException("Patient not found"));

            validatePatientHospital(patient, hospitalId);

            UserEntity currentUser = requireCurrentUser();
            String role = getRole(currentUser);

            if ("PATIENT".equals(role)) {
                UserEntity patientAcc = patient != null ? patient.getAccount() : null;
                UUID patientUserId = patientAcc != null ? patientAcc.getId() : null;
                if (patientUserId == null || currentUser == null || !patientUserId.equals(currentUser.getId())) {
                    throw new AccessDeniedException("You are not authorized to view these prescriptions");
                }

                return prescriptionRepo.findByPatientIdAndPatientHospitalId(validPatientId, hospitalId)
                        .stream()
                        .map(prescriptionMapper::toResponse)
                        .collect(Collectors.toList());
            }

            if ("DOCTOR".equals(role)) {
                DoctorEntity currentDoctor = doctorService.findDoctorEntityByAccountId(currentUser.getId())
                        .orElseThrow(() -> new AccessDeniedException("Authenticated doctor not found"));

                validateDoctorHospital(currentDoctor, hospitalId);

                patientConsentService.verifyConsentForDoctor(
                        validPatientId,
                        currentDoctor.getId(),
                        org.proj.entity.PatientConsentEntity.ConsentType.PRESCRIPTION_ACCESS,
                        "PRESCRIPTIONS"
                );

                return prescriptionRepo.findByPatientIdAndPatientHospitalId(validPatientId, hospitalId)
                        .stream()
                        .filter(p -> p.getDoctor() != null && currentDoctor.getId().equals(p.getDoctor().getId()))
                        .map(prescriptionMapper::toResponse)
                        .collect(Collectors.toList());
            }

            Optional.of(role)
                    .filter(r -> "ADMIN".equals(r) || "RECEPTIONIST".equals(r))
                    .orElseThrow(() -> new AccessDeniedException("You are not authorized to view prescriptions"));

            return prescriptionRepo.findByPatientIdAndPatientHospitalId(validPatientId, hospitalId)
                    .stream()
                    .map(prescriptionMapper::toResponse)
                    .collect(Collectors.toList());
        } catch (AccessDeniedException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            String msg = e.getMessage() != null && !e.getMessage().isBlank()
                    ? e.getMessage()
                    : "Unable to retrieve patient prescriptions";
            throw new RuntimeException(msg, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<PrescriptionResponse> getDoctorPrescriptions(UUID doctorId) {
        try {
            UUID validDoctorId = Optional.ofNullable(doctorId)
                    .orElseThrow(() -> new IllegalArgumentException("Doctor ID is required"));

            UUID hospitalId = requireHospital();

            DoctorEntity doctor = Optional.ofNullable(doctorService.findDoctorById(validDoctorId))
                    .orElseThrow(() -> new IllegalArgumentException("Doctor not found"));

            validateDoctorHospital(doctor, hospitalId);

            UserEntity currentUser = requireCurrentUser();
            String role = getRole(currentUser);

            if ("DOCTOR".equals(role)) {
                UserEntity docAcc = doctor != null ? doctor.getAccount() : null;
                UUID docUserId = docAcc != null ? docAcc.getId() : null;
                if (docUserId == null || currentUser == null || !docUserId.equals(currentUser.getId())) {
                    throw new AccessDeniedException("You are not authorized to view these prescriptions");
                }
            } else if (!"ADMIN".equals(role)) {
                throw new AccessDeniedException("You are not authorized to view doctor prescriptions");
            }

            return prescriptionRepo.findByDoctorIdAndDoctorHospitalId(validDoctorId, hospitalId)
                    .stream()
                    .map(prescriptionMapper::toResponse)
                    .collect(Collectors.toList());
        } catch (AccessDeniedException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            String msg = e.getMessage() != null && !e.getMessage().isBlank()
                    ? e.getMessage()
                    : "Unable to retrieve doctor prescriptions";
            throw new RuntimeException(msg, e);
        }
    }

    private void validateViewAccess(PrescriptionEntity prescription, UUID hospitalId) {
        if (prescription == null || prescription.getAppointment() == null) {
            throw new IllegalArgumentException("Prescription not found");
        }

        validateAppointmentTenant(prescription.getAppointment(), hospitalId);

        UserEntity currentUser = requireCurrentUser();
        String role = getRole(currentUser);

        if ("ADMIN".equals(role) || "RECEPTIONIST".equals(role)) {
            return;
        }

        if ("PATIENT".equals(role)) {
            UserEntity patientAcc = (prescription.getPatient() != null) ? prescription.getPatient().getAccount() : null;
            UUID patientUserId = (patientAcc != null) ? patientAcc.getId() : null;
            if (patientUserId == null || currentUser == null || !patientUserId.equals(currentUser.getId())) {
                throw new AccessDeniedException("You are not authorized to view this prescription");
            }
            return;
        }

        if ("DOCTOR".equals(role)) {
            UserEntity docAcc = (prescription.getDoctor() != null) ? prescription.getDoctor().getAccount() : null;
            UUID docUserId = (docAcc != null) ? docAcc.getId() : null;
            if (docUserId == null || currentUser == null || !docUserId.equals(currentUser.getId())) {
                throw new AccessDeniedException("You are not authorized to view this prescription");
            }
            return;
        }

        throw new AccessDeniedException("You are not authorized to view prescriptions");
    }

    private void validateCreateAccess(DoctorEntity appointmentDoctor, UUID hospitalId) {
        if (appointmentDoctor == null) {
            throw new AccessDeniedException("Appointment doctor is required");
        }

        validateDoctorHospital(appointmentDoctor, hospitalId);

        UserEntity currentUser = requireCurrentUser();
        String role = getRole(currentUser);

        if ("ADMIN".equals(role)) {
            return;
        }

        if ("DOCTOR".equals(role)) {
            DoctorEntity currentDoctor = doctorService.findDoctorEntityByAccountId(currentUser.getId())
                    .orElseThrow(() -> new AccessDeniedException("Authenticated doctor not found"));

            validateDoctorHospital(currentDoctor, hospitalId);

            if (!currentDoctor.getId().equals(appointmentDoctor.getId())) {
                throw new AccessDeniedException("You are not authorized to create prescriptions for this consultation");
            }
            return;
        }

        throw new AccessDeniedException("Only doctors or admins can create prescriptions");
    }

    private void validateAppointmentTenant(AppointmentEntity appointment, UUID hospitalId) {
        if (appointment == null) {
            throw new IllegalArgumentException("Appointment cannot be null");
        }

        UUID appHospId = (appointment.getHospital() != null) ? appointment.getHospital().getId() : null;
        if (hospitalId == null || !hospitalId.equals(appHospId)) {
            throw new AccessDeniedException("You are not authorized to access an appointment from another hospital");
        }

        UUID patientHospId = (appointment.getPatient() != null && appointment.getPatient().getHospital() != null)
                ? appointment.getPatient().getHospital().getId()
                : null;
        if (!hospitalId.equals(patientHospId)) {
            throw new AccessDeniedException("Appointment patient does not belong to the current hospital");
        }

        UUID docHospId = (appointment.getDoctor() != null && appointment.getDoctor().getHospital() != null)
                ? appointment.getDoctor().getHospital().getId()
                : null;
        if (!hospitalId.equals(docHospId)) {
            throw new AccessDeniedException("Appointment doctor does not belong to the current hospital");
        }

        UUID deptHospId = (appointment.getDepartment() != null && appointment.getDepartment().getHospital() != null)
                ? appointment.getDepartment().getHospital().getId()
                : null;
        if (!hospitalId.equals(deptHospId)) {
            throw new AccessDeniedException("Appointment department does not belong to the current hospital");
        }
    }

    private void validateMedicalRecord(MedicalRecordEntity medicalRecord, AppointmentEntity appointment, UUID hospitalId) {
        if (medicalRecord == null) {
            throw new IllegalArgumentException("Medical record not found");
        }

        if (medicalRecord.getPatient() == null || medicalRecord.getDoctor() == null) {
            throw new AccessDeniedException("Invalid medical record ownership");
        }

        validatePatientHospital(medicalRecord.getPatient(), hospitalId);
        validateDoctorHospital(medicalRecord.getDoctor(), hospitalId);

        UUID recordPatientId = medicalRecord.getPatient().getId();
        UUID appPatientId = (appointment != null && appointment.getPatient() != null) ? appointment.getPatient().getId() : null;
        if (recordPatientId == null || !recordPatientId.equals(appPatientId)) {
            throw new IllegalArgumentException("Medical record does not belong to the appointment's patient");
        }

        UUID recordDoctorId = medicalRecord.getDoctor().getId();
        UUID appDoctorId = (appointment != null && appointment.getDoctor() != null) ? appointment.getDoctor().getId() : null;
        if (recordDoctorId == null || !recordDoctorId.equals(appDoctorId)) {
            throw new IllegalArgumentException("Medical record does not belong to the appointment's doctor");
        }

        UUID recordAppId = (medicalRecord.getAppointment() != null) ? medicalRecord.getAppointment().getId() : null;
        UUID appId = (appointment != null) ? appointment.getId() : null;
        if (recordAppId == null || !recordAppId.equals(appId)) {
            throw new IllegalArgumentException("Medical record does not belong to this appointment");
        }

        UUID recordHospId = (medicalRecord.getAppointment() != null && medicalRecord.getAppointment().getHospital() != null)
                ? medicalRecord.getAppointment().getHospital().getId()
                : null;
        if (hospitalId == null || !hospitalId.equals(recordHospId)) {
            throw new AccessDeniedException("Medical record belongs to another hospital");
        }
    }

    private void validatePatientHospital(PatientEntity patient, UUID hospitalId) {
        UUID patHospId = (patient != null && patient.getHospital() != null) ? patient.getHospital().getId() : null;
        if (hospitalId == null || !hospitalId.equals(patHospId)) {
            throw new AccessDeniedException("Patient belongs to another hospital");
        }
    }

    private void validateDoctorHospital(DoctorEntity doctor, UUID hospitalId) {
        UUID docHospId = (doctor != null && doctor.getHospital() != null) ? doctor.getHospital().getId() : null;
        if (hospitalId == null || !hospitalId.equals(docHospId)) {
            throw new AccessDeniedException("Doctor belongs to another hospital");
        }
    }

    private UUID requireHospital() {
        return Optional.ofNullable(tenantContextService.getCurrentUserHospitalId())
                .orElseThrow(() -> new AccessDeniedException("Hospital context is required"));
    }

    private UserEntity requireCurrentUser() {
        return Optional.ofNullable(SecurityUtils.getCurrentUser())
                .orElseThrow(() -> new AccessDeniedException("Authenticated user not found"));
    }

    private String getRole(UserEntity user) {
        String roleName = (user != null && user.getRole() != null && user.getRole().getRoleName() != null)
                ? user.getRole().getRoleName().trim().toUpperCase(java.util.Locale.ROOT)
                : null;
        if (roleName == null || roleName.isEmpty()) {
            throw new AccessDeniedException("User role is not available");
        }
        return roleName;
    }

    @Override
    @Transactional
    public PrescriptionResponse updatePrescription(UUID id, PrescriptionRequest request) {
        try {
            if (id == null) {
                throw new IllegalArgumentException("Prescription ID is required");
            }
            if (request == null) {
                throw new IllegalArgumentException("Prescription request cannot be null");
            }

            PrescriptionEntity prescription = prescriptionRepo.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Prescription not found with ID: " + id));

            UserEntity currentUser = requireCurrentUser();
            String role = getRole(currentUser);

            if ("DOCTOR".equals(role)) {
                UserEntity docAcc = (prescription.getDoctor() != null) ? prescription.getDoctor().getAccount() : null;
                UUID docUserId = (docAcc != null) ? docAcc.getId() : null;
                if (docUserId == null || !docUserId.equals(currentUser.getId())) {
                    throw new AccessDeniedException("You are not authorized to edit this prescription");
                }
            } else if (!"ADMIN".equals(role)) {
                throw new AccessDeniedException("You are not authorized to edit prescriptions");
            }

            if (request.getInstructions() != null) {
                prescription.setInstructions(request.getInstructions());
            }

            if (request.getMedicines() != null) {
                prescription.getMedicines().clear();
                request.getMedicines().stream()
                        .filter(medReq -> medReq.getMedicineName() != null && !medReq.getMedicineName().isBlank())
                        .forEach(medReq -> {
                            var medEntity = org.proj.entity.PrescriptionMedicineEntity.builder()
                                    .medicineName(medReq.getMedicineName().trim())
                                    .dosage(medReq.getDosage())
                                    .frequency(medReq.getFrequency())
                                    .duration(medReq.getDuration())
                                    .instructions(medReq.getInstructions())
                                    .prescription(prescription)
                                    .build();
                            prescription.getMedicines().add(medEntity);
                        });
            }

            PrescriptionEntity saved = prescriptionRepo.save(prescription);
            return prescriptionMapper.toResponse(saved);
        } catch (AccessDeniedException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            String msg = e.getMessage() != null && !e.getMessage().isBlank()
                    ? e.getMessage()
                    : "Unable to update prescription";
            throw new RuntimeException(msg, e);
        }
    }
}
