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

    @Override
    @Transactional
    public PrescriptionResponse createPrescription(PrescriptionRequest request) {

        if (request == null || request.getAppointmentId() == null) {
            throw new IllegalArgumentException("Appointment ID is required");
        }

        UUID hospitalId = requireHospital();

        AppointmentEntity appointment =
                appointmentService.findAppointmentById(
                        request.getAppointmentId());

        validateAppointmentTenant(appointment, hospitalId);

        if (appointment.getAppointmentStatus()
                != AppointmentEntity.AppointmentStatus.IN_PROGRESS) {

            throw new IllegalArgumentException(
                    "Prescription can only be created during an active consultation");
        }

        PatientEntity patient = appointment.getPatient();
        DoctorEntity doctor = appointment.getDoctor();

        validateCreateAccess(doctor, hospitalId);

        MedicalRecordEntity medicalRecord = null;

        if (request.getMedicalRecordId() != null) {

            medicalRecord =
                    medicalRecordService.findMedicalRecordEntityById(
                            request.getMedicalRecordId());

            validateMedicalRecord(
                    medicalRecord,
                    appointment,
                    hospitalId);
        }

        PrescriptionEntity entity =
                prescriptionMapper.toEntity(request);

        entity.setPatient(patient);
        entity.setDoctor(doctor);
        entity.setAppointment(appointment);
        entity.setMedicalRecord(medicalRecord);
        entity.setPrescriptionDate(LocalDate.now());

        PrescriptionEntity saved =
                prescriptionRepo.save(entity);

        notificationService.createNotification(
                patient.getAccount(),
                "Prescription Created",
                "A new prescription has been created for you by Dr. "
                        + doctor.getAccount().getFirstName()
                        + " "
                        + doctor.getAccount().getLastName() + ".",
                NotificationType.PRESCRIPTION_CREATED
        );

        return prescriptionMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PrescriptionResponse getPrescriptionById(UUID id) {

        if (id == null) {
            throw new IllegalArgumentException(
                    "Prescription ID is required");
        }

        UUID hospitalId = requireHospital();

        PrescriptionEntity prescription =
                prescriptionRepo
                        .findByIdAndAppointmentHospitalId(id, hospitalId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Prescription not found"));

        validateViewAccess(prescription, hospitalId);

        return prescriptionMapper.toResponse(prescription);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PrescriptionResponse> getPatientPrescriptions(
            UUID patientId) {

        if (patientId == null) {
            throw new IllegalArgumentException(
                    "Patient ID is required");
        }

        UUID hospitalId = requireHospital();

        PatientEntity patient =
                patientService.findPatientById(patientId);

        validatePatientHospital(patient, hospitalId);

        UserEntity currentUser = requireCurrentUser();

        String role = getRole(currentUser);

        if ("PATIENT".equals(role)) {

            if (patient.getAccount() == null
                    || !currentUser.getId().equals(
                            patient.getAccount().getId())) {

                throw new AccessDeniedException(
                        "You are not authorized to view these prescriptions");
            }

            return prescriptionRepo
                    .findByPatientIdAndPatientHospitalId(
                            patientId,
                            hospitalId)
                    .stream()
                    .map(prescriptionMapper::toResponse)
                    .collect(Collectors.toList());
        }

        if ("DOCTOR".equals(role)) {

            DoctorEntity currentDoctor =
                    doctorService
                            .findDoctorEntityByAccountId(
                                    currentUser.getId())
                            .orElseThrow(() ->
                                    new AccessDeniedException(
                                            "Authenticated doctor not found"));

            validateDoctorHospital(
                    currentDoctor,
                    hospitalId);

            return prescriptionRepo
                    .findByPatientIdAndPatientHospitalId(
                            patientId,
                            hospitalId)
                    .stream()
                    .filter(p ->
                            p.getDoctor() != null
                                    && currentDoctor.getId().equals(
                                    p.getDoctor().getId()))
                    .map(prescriptionMapper::toResponse)
                    .collect(Collectors.toList());
        }

        if ("ADMIN".equals(role)
                || "RECEPTIONIST".equals(role)) {

            return prescriptionRepo
                    .findByPatientIdAndPatientHospitalId(
                            patientId,
                            hospitalId)
                    .stream()
                    .map(prescriptionMapper::toResponse)
                    .collect(Collectors.toList());
        }

        throw new AccessDeniedException(
                "You are not authorized to view prescriptions");
    }

    @Override
    @Transactional(readOnly = true)
    public List<PrescriptionResponse> getDoctorPrescriptions(
            UUID doctorId) {

        if (doctorId == null) {
            throw new IllegalArgumentException(
                    "Doctor ID is required");
        }

        UUID hospitalId = requireHospital();

        DoctorEntity doctor =
                doctorService.findDoctorById(doctorId);

        validateDoctorHospital(
                doctor,
                hospitalId);

        UserEntity currentUser = requireCurrentUser();

        String role = getRole(currentUser);

        if ("DOCTOR".equals(role)) {

            if (doctor.getAccount() == null
                    || !currentUser.getId().equals(
                            doctor.getAccount().getId())) {

                throw new AccessDeniedException(
                        "You are not authorized to view these prescriptions");
            }
        } else if (!"ADMIN".equals(role)) {

            throw new AccessDeniedException(
                    "You are not authorized to view doctor prescriptions");
        }

        return prescriptionRepo
                .findByDoctorIdAndDoctorHospitalId(
                        doctorId,
                        hospitalId)
                .stream()
                .map(prescriptionMapper::toResponse)
                .collect(Collectors.toList());
    }

    private void validateViewAccess(
            PrescriptionEntity prescription,
            UUID hospitalId) {

        if (prescription == null
                || prescription.getAppointment() == null) {

            throw new IllegalArgumentException(
                    "Prescription not found");
        }

        validateAppointmentTenant(
                prescription.getAppointment(),
                hospitalId);

        UserEntity currentUser =
                requireCurrentUser();

        String role = getRole(currentUser);

        if ("ADMIN".equals(role)
                || "RECEPTIONIST".equals(role)) {
            return;
        }

        if ("PATIENT".equals(role)) {

            if (prescription.getPatient() == null
                    || prescription.getPatient().getAccount() == null
                    || !currentUser.getId().equals(
                    prescription.getPatient()
                            .getAccount()
                            .getId())) {

                throw new AccessDeniedException(
                        "You are not authorized to view this prescription");
            }

            return;
        }

        if ("DOCTOR".equals(role)) {

            if (prescription.getDoctor() == null
                    || prescription.getDoctor().getAccount() == null
                    || !currentUser.getId().equals(
                    prescription.getDoctor()
                            .getAccount()
                            .getId())) {

                throw new AccessDeniedException(
                        "You are not authorized to view this prescription");
            }

            return;
        }

        throw new AccessDeniedException(
                "You are not authorized to view prescriptions");
    }

    private void validateCreateAccess(
            DoctorEntity appointmentDoctor,
            UUID hospitalId) {

        if (appointmentDoctor == null) {
            throw new AccessDeniedException(
                    "Appointment doctor is required");
        }

        validateDoctorHospital(
                appointmentDoctor,
                hospitalId);

        UserEntity currentUser =
                requireCurrentUser();

        String role = getRole(currentUser);

        if ("ADMIN".equals(role)) {
            return;
        }

        if ("DOCTOR".equals(role)) {

            DoctorEntity currentDoctor =
                    doctorService
                            .findDoctorEntityByAccountId(
                                    currentUser.getId())
                            .orElseThrow(() ->
                                    new AccessDeniedException(
                                            "Authenticated doctor not found"));

            validateDoctorHospital(
                    currentDoctor,
                    hospitalId);

            if (!currentDoctor.getId().equals(
                    appointmentDoctor.getId())) {

                throw new AccessDeniedException(
                        "You are not authorized to create prescriptions for this consultation");
            }

            return;
        }

        throw new AccessDeniedException(
                "Only doctors or admins can create prescriptions");
    }

    private void validateAppointmentTenant(
            AppointmentEntity appointment,
            UUID hospitalId) {

        if (appointment == null
                || appointment.getHospital() == null
                || appointment.getHospital().getId() == null
                || !hospitalId.equals(
                appointment.getHospital().getId())) {

            throw new AccessDeniedException(
                    "You are not authorized to access an appointment from another hospital");
        }

        if (appointment.getPatient() == null
                || appointment.getPatient().getHospital() == null
                || appointment.getPatient().getHospital().getId() == null
                || !hospitalId.equals(
                appointment.getPatient()
                        .getHospital()
                        .getId())) {

            throw new AccessDeniedException(
                    "Appointment patient does not belong to the current hospital");
        }

        if (appointment.getDoctor() == null
                || appointment.getDoctor().getHospital() == null
                || appointment.getDoctor().getHospital().getId() == null
                || !hospitalId.equals(
                appointment.getDoctor()
                        .getHospital()
                        .getId())) {

            throw new AccessDeniedException(
                    "Appointment doctor does not belong to the current hospital");
        }

        if (appointment.getDepartment() == null
                || appointment.getDepartment().getHospital() == null
                || appointment.getDepartment().getHospital().getId() == null
                || !hospitalId.equals(
                appointment.getDepartment()
                        .getHospital()
                        .getId())) {

            throw new AccessDeniedException(
                    "Appointment department does not belong to the current hospital");
        }
    }

    private void validateMedicalRecord(
            MedicalRecordEntity medicalRecord,
            AppointmentEntity appointment,
            UUID hospitalId) {

        if (medicalRecord == null) {
            throw new IllegalArgumentException(
                    "Medical record not found");
        }

        if (medicalRecord.getPatient() == null
                || medicalRecord.getDoctor() == null) {

            throw new AccessDeniedException(
                    "Invalid medical record ownership");
        }

        validatePatientHospital(
                medicalRecord.getPatient(),
                hospitalId);

        validateDoctorHospital(
                medicalRecord.getDoctor(),
                hospitalId);

        if (!medicalRecord.getPatient().getId().equals(
                appointment.getPatient().getId())) {

            throw new IllegalArgumentException(
                    "Medical record does not belong to the appointment's patient");
        }

        if (!medicalRecord.getDoctor().getId().equals(
                appointment.getDoctor().getId())) {

            throw new IllegalArgumentException(
                    "Medical record does not belong to the appointment's doctor");
        }

        if (medicalRecord.getAppointment() == null
                || !medicalRecord.getAppointment().getId().equals(
                appointment.getId())) {

            throw new IllegalArgumentException(
                    "Medical record does not belong to this appointment");
        }

        if (medicalRecord.getAppointment().getHospital() == null
                || !hospitalId.equals(
                medicalRecord.getAppointment()
                        .getHospital()
                        .getId())) {

            throw new AccessDeniedException(
                    "Medical record belongs to another hospital");
        }
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
                    "Patient belongs to another hospital");
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
                    "Doctor belongs to another hospital");
        }
    }

    private UUID requireHospital() {

        UUID hospitalId =
                tenantContextService.getCurrentUserHospitalId();

        if (hospitalId == null) {
            throw new AccessDeniedException(
                    "Hospital context is required");
        }

        return hospitalId;
    }

    private UserEntity requireCurrentUser() {

        UserEntity currentUser =
                SecurityUtils.getCurrentUser();

        if (currentUser == null) {
            throw new AccessDeniedException(
                    "Authenticated user not found");
        }

        return currentUser;
    }

    private String getRole(UserEntity user) {

        if (user.getRole() == null
                || user.getRole().getRoleName() == null) {

            throw new AccessDeniedException(
                    "User role is not available");
        }

        return user.getRole()
                .getRoleName()
                .toUpperCase();
    }
}