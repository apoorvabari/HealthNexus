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

    @Override
    @Transactional
    public MedicalRecordResponse createMedicalRecord(MedicalRecordRequest request) {

        UUID hospitalId = requireCurrentHospital();

        PatientEntity patient = patientService.findPatientById(request.getPatientId());

        if (patient == null) {
            throw new IllegalArgumentException("Patient not found");
        }

        validatePatientHospital(patient, hospitalId);

        DoctorEntity doctor = doctorService.findDoctorById(request.getDoctorId());

        if (doctor == null) {
            throw new IllegalArgumentException("Doctor not found");
        }

        validateDoctorHospital(doctor, hospitalId);

        UserEntity currentUser = requireCurrentUser();
        String role = getRole(currentUser);

        /*
         * DOCTOR can create records only for himself.
         */
        if ("DOCTOR".equals(role)) {

            DoctorEntity currentDoctor =
                    doctorService.findDoctorEntityByAccountId(currentUser.getId())
                            .orElseThrow(() ->
                                    new AccessDeniedException(
                                            "Doctor profile not found"
                                    ));

            if (!currentDoctor.getId().equals(doctor.getId())) {
                throw new AccessDeniedException(
                        "You are not authorized to create records for another doctor"
                );
            }

            validateDoctorHospital(currentDoctor, hospitalId);
        }

        AppointmentEntity appointment = null;

        if (request.getAppointmentId() != null) {

            appointment =
                    appointmentService.findAppointmentById(
                            request.getAppointmentId()
                    );

            if (appointment == null) {
                throw new IllegalArgumentException("Appointment not found");
            }

            validateAppointmentHospital(appointment, hospitalId);

            if (appointment.getPatient() == null ||
                    !appointment.getPatient().getId().equals(patient.getId())) {

                throw new IllegalArgumentException(
                        "Appointment does not belong to the specified patient"
                );
            }

            if (appointment.getDoctor() == null ||
                    !appointment.getDoctor().getId().equals(doctor.getId())) {

                throw new IllegalArgumentException(
                        "Appointment does not belong to the specified doctor"
                );
            }

            if (appointment.getAppointmentStatus() != AppointmentStatus.IN_PROGRESS) {

                throw new IllegalArgumentException(
                        "Medical record can only be created during an active consultation"
                );
            }
        }

        MedicalRecordEntity entity =
                medicalRecordMapper.toEntity(request);

        entity.setPatient(patient);
        entity.setDoctor(doctor);
        entity.setAppointment(appointment);

        MedicalRecordEntity saved =
                medicalRecordRepo.save(entity);

        notificationService.createNotification(
                patient.getAccount(),
                "Medical Record Available",
                "A new medical record has been created for you by Dr. "
                        + doctor.getAccount().getFirstName()
                        + " "
                        + doctor.getAccount().getLastName()
                        + ".",
                NotificationType.MEDICAL_RECORD_CREATED
        );

        return medicalRecordMapper.toResponse(saved);
    }

    @Override
    public MedicalRecordResponse getMedicalRecordById(UUID id) {

        UUID hospitalId = requireCurrentHospital();
        UserEntity currentUser = requireCurrentUser();

        MedicalRecordEntity record =
                findTenantScopedRecord(id, hospitalId);

        verifyViewAccess(record, currentUser, hospitalId);

        return medicalRecordMapper.toResponse(record);
    }

    /**
     * Internal use by other services.
     *
     * IMPORTANT:
     * This method is tenant-scoped so another hospital's
     * medical record cannot be loaded by ID.
     */
    @Override
    public MedicalRecordEntity findMedicalRecordEntityById(UUID id) {

        UUID hospitalId = requireCurrentHospital();

        return findTenantScopedRecord(id, hospitalId);
    }

    @Override
    public List<MedicalRecordResponse> getPatientMedicalRecords(UUID patientId) {

        UUID hospitalId = requireCurrentHospital();
        UserEntity currentUser = requireCurrentUser();

        PatientEntity patient =
                patientService.findPatientById(patientId);

        if (patient == null) {
            throw new IllegalArgumentException("Patient not found");
        }

        validatePatientHospital(patient, hospitalId);

        String role = getRole(currentUser);

        /*
         * PATIENT -> own records only.
         */
        if ("PATIENT".equals(role)) {

            if (patient.getAccount() == null ||
                    !patient.getAccount().getId().equals(currentUser.getId())) {

                throw new AccessDeniedException(
                        "You are not authorized to view this patient's medical records"
                );
            }
        }

        /*
         * DOCTOR -> only records belonging to the current doctor.
         */
        List<MedicalRecordEntity> records =
                medicalRecordRepo.findByPatientIdAndPatientHospitalId(
                        patientId,
                        hospitalId
                );

        if ("DOCTOR".equals(role)) {

            DoctorEntity currentDoctor =
                    doctorService.findDoctorEntityByAccountId(currentUser.getId())
                            .orElseThrow(() ->
                                    new AccessDeniedException(
                                            "Doctor profile not found"
                                    ));

            validateDoctorHospital(currentDoctor, hospitalId);

            records = records.stream()
                    .filter(record ->
                            record.getDoctor() != null &&
                            record.getDoctor().getId()
                                    .equals(currentDoctor.getId()))
                    .collect(Collectors.toList());
        }

        /*
         * ADMIN / RECEPTIONIST:
         * All records for patients in their hospital.
         *
         * Receptionist is not exposed by the current controller,
         * but this keeps service-level tenant enforcement correct.
         */
        return records.stream()
                .map(medicalRecordMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<MedicalRecordResponse> getDoctorMedicalRecords(UUID doctorId) {

        UUID hospitalId = requireCurrentHospital();
        UserEntity currentUser = requireCurrentUser();

        DoctorEntity doctor =
                doctorService.findDoctorById(doctorId);

        if (doctor == null) {
            throw new IllegalArgumentException("Doctor not found");
        }

        validateDoctorHospital(doctor, hospitalId);

        String role = getRole(currentUser);

        /*
         * DOCTOR -> own records only.
         */
        if ("DOCTOR".equals(role)) {

            if (doctor.getAccount() == null ||
                    !doctor.getAccount().getId().equals(currentUser.getId())) {

                throw new AccessDeniedException(
                        "You are not authorized to view this doctor's medical records"
                );
            }
        }

        List<MedicalRecordEntity> records =
                medicalRecordRepo.findByDoctorIdAndDoctorHospitalId(
                        doctorId,
                        hospitalId
                );

        return records.stream()
                .map(medicalRecordMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public MedicalRecordResponse getMedicalRecordByAppointment(
            UUID appointmentId
    ) {

        UUID hospitalId = requireCurrentHospital();

        /*
         * Appointment itself must belong to current hospital.
         */
        AppointmentEntity appointment =
                appointmentService.findAppointmentById(appointmentId);

        if (appointment == null) {
            throw new IllegalArgumentException("Appointment not found");
        }

        validateAppointmentHospital(appointment, hospitalId);

        List<MedicalRecordEntity> records =
                medicalRecordRepo
                        .findByAppointmentIdAndAppointmentHospitalId(
                                appointmentId,
                                hospitalId
                        );

        if (records.isEmpty()) {
            return null;
        }

        MedicalRecordEntity record = records.get(0);

        /*
         * Protect against inconsistent DB relationships.
         */
        if (record.getPatient() == null ||
                record.getDoctor() == null) {

            throw new IllegalArgumentException(
                    "Invalid medical record relationship"
            );
        }

        validatePatientHospital(record.getPatient(), hospitalId);
        validateDoctorHospital(record.getDoctor(), hospitalId);

        return medicalRecordMapper.toResponse(record);
    }

    private MedicalRecordEntity findTenantScopedRecord(
            UUID id,
            UUID hospitalId
    ) {

        /*
         * Patient is the primary tenant boundary.
         */
        return medicalRecordRepo
                .findByIdAndPatientHospitalId(id, hospitalId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Medical Record not found"
                        ));
    }

    private void verifyViewAccess(
            MedicalRecordEntity record,
            UserEntity currentUser,
            UUID hospitalId
    ) {

        if (record == null) {
            throw new IllegalArgumentException(
                    "Medical Record not found"
            );
        }

        if (record.getPatient() == null ||
                record.getDoctor() == null) {

            throw new AccessDeniedException(
                    "Invalid medical record ownership"
            );
        }

        validatePatientHospital(record.getPatient(), hospitalId);
        validateDoctorHospital(record.getDoctor(), hospitalId);

        String role = getRole(currentUser);

        if ("ADMIN".equals(role) ||
                "RECEPTIONIST".equals(role)) {

            return;
        }

        if ("PATIENT".equals(role)) {

            if (record.getPatient().getAccount() == null ||
                    !record.getPatient()
                            .getAccount()
                            .getId()
                            .equals(currentUser.getId())) {

                throw new AccessDeniedException(
                        "You can only access your own medical records"
                );
            }

            return;
        }

        if ("DOCTOR".equals(role)) {

            DoctorEntity currentDoctor =
                    doctorService.findDoctorEntityByAccountId(
                            currentUser.getId()
                    )
                    .orElseThrow(() ->
                            new AccessDeniedException(
                                    "Doctor profile not found"
                            ));

            validateDoctorHospital(currentDoctor, hospitalId);

            if (!record.getDoctor().getId()
                    .equals(currentDoctor.getId())) {

                throw new AccessDeniedException(
                        "You can only access medical records for your own consultations"
                );
            }
        }
    }

    private UUID requireCurrentHospital() {

        UUID hospitalId =
                tenantContextService.getCurrentUserHospitalId();

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

        if (user.getRole() == null ||
                user.getRole().getRoleName() == null) {

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
            UUID hospitalId
    ) {

        if (patient.getHospital() == null ||
                patient.getHospital().getId() == null ||
                !hospitalId.equals(patient.getHospital().getId())) {

            throw new AccessDeniedException(
                    "You are not authorized to access patient data from another hospital"
            );
        }
    }

    private void validateDoctorHospital(
            DoctorEntity doctor,
            UUID hospitalId
    ) {

        if (doctor.getHospital() == null ||
                doctor.getHospital().getId() == null ||
                !hospitalId.equals(doctor.getHospital().getId())) {

            throw new AccessDeniedException(
                    "You are not authorized to access doctor data from another hospital"
            );
        }
    }

    private void validateAppointmentHospital(
            AppointmentEntity appointment,
            UUID hospitalId
    ) {

        if (appointment.getHospital() == null ||
                appointment.getHospital().getId() == null ||
                !hospitalId.equals(appointment.getHospital().getId())) {

            throw new AccessDeniedException(
                    "You are not authorized to access an appointment from another hospital"
            );
        }
    }
}
