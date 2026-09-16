package org.proj.service.impl;

import lombok.RequiredArgsConstructor;
import org.proj.dto.AppointmentRequest;
import org.proj.dto.PatientAppointmentRequest;
import org.proj.dto.AppointmentResponse;
import org.proj.entity.AppointmentEntity;
import org.proj.entity.DepartmentEntity;
import org.proj.entity.DoctorEntity;
import org.proj.entity.HospitalEntity;
import org.proj.entity.NotificationEntity.NotificationType;
import org.proj.entity.PatientConsentEntity.ConsentType;
import org.proj.entity.PatientEntity;
import org.proj.entity.ReceptionistEntity;
import org.proj.entity.UserEntity;
import org.proj.mapper.AppointmentMapper;
import org.proj.repository.AppointmentRepo;
import org.proj.security.SecurityUtils;
import org.proj.service.AppointmentService;
import org.proj.service.DepartmentService;
import org.proj.service.DoctorService;
import org.proj.service.HospitalService;
import org.proj.service.NotificationService;
import org.proj.service.PatientConsentService;
import org.proj.service.PatientService;
import org.proj.service.ReceptionistService;
import org.proj.service.TenantContextService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AppointmentServiceImpl implements AppointmentService {

    private final AppointmentRepo appointmentRepo;
    private final HospitalService hospitalService;
    private final DepartmentService departmentService;
    private final DoctorService doctorService;
    private final PatientService patientService;
    private final ReceptionistService receptionistService;
    private final AppointmentMapper appointmentMapper;
    private final NotificationService notificationService;
    private final PatientConsentService patientConsentService;
    private final TenantContextService tenantContextService;

    @Override
    @Transactional
    public AppointmentResponse createAppointment(AppointmentRequest request) {
        try {
            UserEntity currentUser = SecurityUtils.getCurrentUser();

            /*
             * NEVER trust hospitalId supplied by frontend.
             * Always derive hospital from authenticated user's tenant.
             */
            UUID currentHospitalId = tenantContextService.getCurrentUserHospitalId();

            if (currentHospitalId == null) {
                throw new AccessDeniedException("Hospital context is required");
            }

            request.setHospitalId(currentHospitalId);

            HospitalEntity hospital =
                    hospitalService.findHospitalById(currentHospitalId);

            DepartmentEntity department =
                    departmentService.findDepartmentById(request.getDepartmentId());

            DoctorEntity doctor =
                    doctorService.findDoctorById(request.getDoctorId());

            PatientEntity patient =
                    patientService.findPatientById(request.getPatientId());

            /*
             * Patient can only create appointment for themselves.
             */
            if (SecurityUtils.isPatient()) {

                if (patient.getAccount() == null
                        || !patient.getAccount().getId()
                        .equals(currentUser.getId())) {

                    throw new AccessDeniedException(
                            "You are not authorized to book an appointment for another patient"
                    );
                }
            }

            /*
             * Doctor can only create appointment for himself.
             */
            if ("DOCTOR".equalsIgnoreCase(
                    SecurityUtils.getCurrentUser().getRole().getRoleName())) {

                DoctorEntity currentDoctor =
                        doctorService.findDoctorEntityByAccountId(currentUser.getId())
                                .orElseThrow(() ->
                                        new AccessDeniedException(
                                                "Doctor profile not found"
                                        ));

                if (!currentDoctor.getId().equals(doctor.getId())) {
                    throw new AccessDeniedException(
                            "Doctors can only book appointments for themselves"
                    );
                }
            }

            ReceptionistEntity receptionist = null;

            if (request.getBookedByReceptionistId() != null) {

                receptionist =
                        receptionistService.findReceptionistById(
                                request.getBookedByReceptionistId()
                        );

                if (receptionist.getHospital() == null
                        || !currentHospitalId.equals(
                        receptionist.getHospital().getId())) {

                    throw new AccessDeniedException(
                            "Receptionist does not belong to your hospital"
                    );
                }
            }

            /*
             * Department must belong to current hospital.
             */
            if (department.getHospital() == null
                    || !currentHospitalId.equals(
                    department.getHospital().getId())) {

                throw new AccessDeniedException(
                        "Department does not belong to your hospital"
                );
            }

            /*
             * Doctor must belong to current hospital.
             */
            if (doctor.getHospital() == null
                    || !currentHospitalId.equals(
                    doctor.getHospital().getId())) {

                throw new AccessDeniedException(
                        "Doctor does not belong to your hospital"
                );
            }

            /*
             * Doctor must belong to selected department.
             */
            if (doctor.getDepartment() == null
                    || !doctor.getDepartment().getId()
                    .equals(department.getId())) {

                throw new IllegalArgumentException(
                        "Doctor does not belong to the selected department"
                );
            }

            /*
             * Patient must belong to current hospital.
             */
            if (patient.getHospital() == null
                    || !currentHospitalId.equals(
                    patient.getHospital().getId())) {

                throw new AccessDeniedException(
                        "Patient does not belong to your hospital"
                );
            }

            /*
             * Hospital-scoped doctor availability.
             */
            if (appointmentRepo
                    .existsByDoctorIdAndAppointmentDateAndAppointmentTimeAndHospitalId(
                            doctor.getId(),
                            request.getAppointmentDate(),
                            request.getAppointmentTime(),
                            currentHospitalId)) {

                throw new IllegalArgumentException(
                        "Doctor is not available at the selected date and time"
                );
            }

            patientConsentService.verifyConsent(
                    patient.getId(),
                    ConsentType.APPOINTMENT_BOOKING
            );

            AppointmentEntity appointment =
                    appointmentMapper.toEntity(
                            request,
                            hospital,
                            department,
                            doctor,
                            patient,
                            receptionist
                    );

            String appointmentNumber;

            do {
                appointmentNumber =
                        generateAppointmentCode(
                                request.getAppointmentDate()
                        );
            } while (
                    appointmentRepo.existsByAppointmentNumber(
                            appointmentNumber
                    )
            );

            appointment.setAppointmentNumber(appointmentNumber);

            AppointmentEntity savedAppointment =
                    appointmentRepo.save(appointment);

            notificationService.createNotification(
                    savedAppointment.getPatient().getAccount(),
                    "Appointment Booked",
                    "Your appointment ("
                            + savedAppointment.getAppointmentNumber()
                            + ") has been booked for "
                            + savedAppointment.getAppointmentDate()
                            + " at "
                            + savedAppointment.getAppointmentTime()
                            + ".",
                    NotificationType.APPOINTMENT_BOOKED
            );

            AppointmentResponse response =
                    appointmentMapper.toResponse(savedAppointment);

            response.setMessage("Appointment booked successfully");

            return response;

        } catch (AccessDeniedException e) {
            throw e;

        } catch (IllegalArgumentException e) {
            throw e;

        } catch (Exception e) {
            throw new RuntimeException(
                    "Unable to book appointment.",
                    e
            );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public AppointmentResponse getAppointmentById(UUID id) {

        try {

            if (id == null) {
                throw new IllegalArgumentException(
                        "Appointment Id is required"
                );
            }

            UserEntity currentUser =
                    SecurityUtils.getCurrentUser();

            AppointmentEntity appointment;

            /*
             * PATIENT → own appointment only.
             */
            if (SecurityUtils.isPatient()) {

                PatientEntity patient =
                        patientService.findPatientByAccountId(
                                currentUser.getId()
                        );

                appointment =
                        appointmentRepo
                                .findByIdAndHospitalId(
                                        id,
                                        patient.getHospital().getId()
                                )
                                .orElseThrow(() -> {
                                    if (appointmentRepo.existsById(id)) {
                                        return new AccessDeniedException("Access denied");
                                    }
                                    return new IllegalArgumentException("Appointment not found");
                                });

                if (appointment.getPatient() == null
                        || appointment.getPatient().getAccount() == null
                        || !appointment.getPatient()
                        .getAccount()
                        .getId()
                        .equals(currentUser.getId())) {

                    throw new AccessDeniedException(
                            "You are not authorized to view this appointment"
                    );
                }

            } else {

                /*
                 * STAFF → same hospital only.
                 */
                UUID hospitalId =
                        requireCurrentHospital();

                appointment =
                        appointmentRepo
                                .findByIdAndHospitalId(id, hospitalId)
                                .orElseThrow(() -> {
                                    if (appointmentRepo.existsById(id)) {
                                        return new AccessDeniedException("Access denied");
                                    }
                                    return new IllegalArgumentException("Appointment not found");
                                });
            }

            /*
             * DOCTOR → own appointments only.
             */
            if ("DOCTOR".equalsIgnoreCase(
                    SecurityUtils.getCurrentUser().getRole().getRoleName())) {

                DoctorEntity doctor =
                        doctorService
                                .findDoctorEntityByAccountId(
                                        currentUser.getId()
                                )
                                .orElseThrow(() ->
                                        new AccessDeniedException(
                                                "Doctor profile not found"
                                        ));

                if (appointment.getDoctor() == null
                        || !appointment.getDoctor()
                        .getId()
                        .equals(doctor.getId())) {

                    throw new AccessDeniedException(
                            "You are not authorized to view another doctor's appointments"
                    );
                }
            }

            return appointmentMapper.toResponse(appointment);

        } catch (AccessDeniedException e) {
            throw e;

        } catch (IllegalArgumentException e) {
            throw e;

        } catch (Exception e) {
            throw new RuntimeException(
                    "Unable to fetch appointment.",
                    e
            );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppointmentResponse> getAllAppointments() {

        try {

            UUID hospitalId =
                    tenantContextService.getCurrentUserHospitalId();

            if (hospitalId == null) {
                throw new AccessDeniedException(
                        "Tenant hospital context is required"
                );
            }

            return appointmentRepo
                    .findByHospitalId(hospitalId)
                    .stream()
                    .map(appointmentMapper::toResponse)
                    .toList();

        } catch (AccessDeniedException e) {
            throw e;

        } catch (Exception e) {
            throw new RuntimeException(
                    "Unable to fetch appointment list.",
                    e
            );
        }
    }

    @Override
    @Transactional
    public AppointmentResponse updateAppointment(
            UUID id,
            AppointmentRequest request) {

        try {

            if (id == null) {
                throw new IllegalArgumentException(
                        "Appointment Id is required"
                );
            }

            UUID currentHospitalId =
                    requireCurrentHospital();

            UserEntity currentUser =
                    SecurityUtils.getCurrentUser();

            /*
             * CRITICAL:
             * Never load appointment globally.
             * Appointment must already belong to current tenant.
             */
            AppointmentEntity appointment =
                    appointmentRepo
                            .findByIdAndHospitalId(
                                    id,
                                    currentHospitalId
                            )
                            .orElseThrow(() -> {
                                    if (appointmentRepo.existsById(id)) {
                                        return new AccessDeniedException("Access denied");
                                    }
                                    return new IllegalArgumentException("Appointment not found");
                                });

            /*
             * PATIENT ownership.
             */
            if (SecurityUtils.isPatient()) {

                if (appointment.getPatient() == null
                        || appointment.getPatient().getAccount() == null
                        || !appointment.getPatient()
                        .getAccount()
                        .getId()
                        .equals(currentUser.getId())) {

                    throw new AccessDeniedException(
                            "You are not authorized to update this appointment"
                    );
                }

                /*
                 * Patient cannot change ownership/tenant relations.
                 */
                if (request.getHospitalId() != null
                        && !request.getHospitalId()
                        .equals(currentHospitalId)) {

                    throw new AccessDeniedException(
                            "Patients cannot change the hospital of an appointment"
                    );
                }

                if (request.getDepartmentId() != null
                        && !request.getDepartmentId()
                        .equals(
                                appointment.getDepartment().getId()
                        )) {

                    throw new AccessDeniedException(
                            "Patients cannot change the department of an appointment"
                    );
                }

                if (request.getDoctorId() != null
                        && !request.getDoctorId()
                        .equals(
                                appointment.getDoctor().getId()
                        )) {

                    throw new AccessDeniedException(
                            "Patients cannot change the doctor of an appointment"
                    );
                }

                if (request.getPatientId() != null
                        && !request.getPatientId()
                        .equals(
                                appointment.getPatient().getId()
                        )) {

                    throw new AccessDeniedException(
                            "Patients cannot change the patient of an appointment"
                    );
                }

                if (request.getAppointmentStatus() != null
                        && request.getAppointmentStatus()
                        != appointment.getAppointmentStatus()) {

                    throw new AccessDeniedException(
                            "Patients cannot change the status of an appointment"
                    );
                }
            }

            /*
             * DOCTOR ownership.
             */
            if ("DOCTOR".equalsIgnoreCase(
                    SecurityUtils.getCurrentUser().getRole().getRoleName())) {

                DoctorEntity currentDoctor =
                        doctorService
                                .findDoctorEntityByAccountId(
                                        currentUser.getId()
                                )
                                .orElseThrow(() ->
                                        new AccessDeniedException(
                                                "Doctor profile not found"
                                        ));

                if (appointment.getDoctor() == null
                        || !appointment.getDoctor()
                        .getId()
                        .equals(currentDoctor.getId())) {

                    throw new AccessDeniedException(
                            "Doctors can only update their own appointments"
                    );
                }
            }

            /*
             * RECEPTIONIST tenant validation.
             */
            if ("RECEPTIONIST".equalsIgnoreCase(
                    SecurityUtils.getCurrentUser().getRole().getRoleName())) {

                ReceptionistEntity receptionist =
                        receptionistService
                                .findReceptionistEntityByAccountId(
                                        currentUser.getId()
                                )
                                .orElseThrow(() ->
                                        new AccessDeniedException(
                                                "Receptionist profile not found"
                                        ));

                if (receptionist.getHospital() == null
                        || !currentHospitalId.equals(
                        receptionist.getHospital().getId())) {

                    throw new AccessDeniedException(
                            "You are not authorized to update appointments outside your hospital"
                    );
                }
            }

            /*
             * Hospital can NEVER be changed.
             */
            if (request.getHospitalId() != null
                    && !request.getHospitalId()
                    .equals(currentHospitalId)) {

                throw new AccessDeniedException(
                        "You cannot move an appointment to a different hospital"
                );
            }

            HospitalEntity hospital =
                    appointment.getHospital();

            /*
             * Resolve department.
             */
            DepartmentEntity department =
                    appointment.getDepartment();

            if (request.getDepartmentId() != null
                    && !request.getDepartmentId()
                    .equals(department.getId())) {

                department =
                        departmentService.findDepartmentById(
                                request.getDepartmentId()
                        );

                if (department.getHospital() == null
                        || !currentHospitalId.equals(
                        department.getHospital().getId())) {

                    throw new AccessDeniedException(
                            "Department does not belong to your hospital"
                    );
                }
            }

            /*
             * Resolve doctor.
             */
            DoctorEntity doctor =
                    appointment.getDoctor();

            if (request.getDoctorId() != null
                    && !request.getDoctorId()
                    .equals(doctor.getId())) {

                doctor =
                        doctorService.findDoctorById(
                                request.getDoctorId()
                        );

                if (doctor.getHospital() == null
                        || !currentHospitalId.equals(
                        doctor.getHospital().getId())) {

                    throw new AccessDeniedException(
                            "Doctor does not belong to your hospital"
                    );
                }
            }

            /*
             * Resolve patient.
             */
            PatientEntity patient =
                    appointment.getPatient();

            if (request.getPatientId() != null
                    && !request.getPatientId()
                    .equals(patient.getId())) {

                patient =
                        patientService.findPatientById(
                                request.getPatientId()
                        );

                if (patient.getHospital() == null
                        || !currentHospitalId.equals(
                        patient.getHospital().getId())) {

                    throw new AccessDeniedException(
                            "Patient does not belong to your hospital"
                    );
                }
            }

            /*
             * Doctor → department consistency.
             */
            if (doctor.getDepartment() == null
                    || !doctor.getDepartment()
                    .getId()
                    .equals(department.getId())) {

                throw new IllegalArgumentException(
                        "Doctor does not belong to the selected department"
                );
            }

            /*
             * Doctor → hospital consistency.
             */
            if (doctor.getHospital() == null
                    || !doctor.getHospital()
                    .getId()
                    .equals(currentHospitalId)) {

                throw new AccessDeniedException(
                        "Doctor does not belong to your hospital"
                );
            }

            /*
             * Patient → hospital consistency.
             */
            if (patient.getHospital() == null
                    || !patient.getHospital()
                    .getId()
                    .equals(currentHospitalId)) {

                throw new AccessDeniedException(
                        "Patient does not belong to your hospital"
                );
            }

            /*
             * Resolve receptionist.
             */
            ReceptionistEntity receptionist =
                    appointment.getBookedByReceptionist();

            if (request.getBookedByReceptionistId() != null
                    && (receptionist == null
                    || !request.getBookedByReceptionistId()
                    .equals(receptionist.getId()))) {

                receptionist =
                        receptionistService.findReceptionistById(
                                request.getBookedByReceptionistId()
                        );

                if (receptionist.getHospital() == null
                        || !currentHospitalId.equals(
                        receptionist.getHospital().getId())) {

                    throw new AccessDeniedException(
                            "Receptionist does not belong to your hospital"
                    );
                }
            }

            LocalDate targetDate =
                    request.getAppointmentDate() != null
                            ? request.getAppointmentDate()
                            : appointment.getAppointmentDate();

            LocalTime targetTime =
                    request.getAppointmentTime() != null
                            ? request.getAppointmentTime()
                            : appointment.getAppointmentTime();

            /*
             * Hospital-scoped availability.
             */
            if (appointmentRepo
                    .existsByDoctorIdAndAppointmentDateAndAppointmentTimeAndIdNotAndHospitalId(
                            doctor.getId(),
                            targetDate,
                            targetTime,
                            id,
                            currentHospitalId)) {

                throw new IllegalArgumentException(
                        "Doctor is not available at the selected date and time"
                );
            }

            appointmentMapper.updateEntity(
                    appointment,
                    request,
                    hospital,
                    department,
                    doctor,
                    patient,
                    receptionist
            );

            /*
             * Force original tenant.
             */
            appointment.setHospital(hospital);

            AppointmentEntity updatedAppointment =
                    appointmentRepo.save(appointment);

            notificationService.createNotification(
                    updatedAppointment.getPatient().getAccount(),
                    "Appointment Updated",
                    "Your appointment ("
                            + updatedAppointment.getAppointmentNumber()
                            + ") has been updated. New date: "
                            + updatedAppointment.getAppointmentDate()
                            + " at "
                            + updatedAppointment.getAppointmentTime()
                            + ".",
                    NotificationType.APPOINTMENT_UPDATED
            );

            AppointmentResponse response =
                    appointmentMapper.toResponse(
                            updatedAppointment
                    );

            response.setMessage(
                    "Appointment updated successfully"
            );

            return response;

        } catch (AccessDeniedException e) {
            throw e;

        } catch (IllegalArgumentException e) {
            throw e;

        } catch (Exception e) {
            throw new RuntimeException(
                    "Unable to update appointment.",
                    e
            );
        }
    }

    @Override
    @Transactional
    public AppointmentResponse updateAppointmentAsPatient(
            UUID id,
            PatientAppointmentRequest request) {

        try {

            if (id == null) {
                throw new IllegalArgumentException(
                        "Appointment Id is required"
                );
            }

            UserEntity currentUser =
                    SecurityUtils.getCurrentUser();

            UUID hospitalId =
                    requireCurrentHospital();

            AppointmentEntity appointment =
                    appointmentRepo
                            .findByIdAndHospitalId(
                                    id,
                                    hospitalId
                            )
                            .orElseThrow(() -> {
                                    if (appointmentRepo.existsById(id)) {
                                        return new AccessDeniedException("Access denied");
                                    }
                                    return new IllegalArgumentException("Appointment not found");
                                });

            if (appointment.getPatient() == null
                    || appointment.getPatient().getAccount() == null
                    || !appointment.getPatient()
                    .getAccount()
                    .getId()
                    .equals(currentUser.getId())) {

                throw new AccessDeniedException(
                        "You are not authorized to update this appointment"
                );
            }

            LocalDate targetDate =
                    request.getAppointmentDate() != null
                            ? request.getAppointmentDate()
                            : appointment.getAppointmentDate();

            LocalTime targetTime =
                    request.getAppointmentTime() != null
                            ? request.getAppointmentTime()
                            : appointment.getAppointmentTime();

            if (appointmentRepo
                    .existsByDoctorIdAndAppointmentDateAndAppointmentTimeAndIdNotAndHospitalId(
                            appointment.getDoctor().getId(),
                            targetDate,
                            targetTime,
                            id,
                            hospitalId)) {

                throw new IllegalArgumentException(
                        "Doctor is not available at the selected date and time"
                );
            }

            if (request.getAppointmentDate() != null) {
                appointment.setAppointmentDate(
                        request.getAppointmentDate()
                );
            }

            if (request.getAppointmentTime() != null) {
                appointment.setAppointmentTime(
                        request.getAppointmentTime()
                );
            }

            if (request.getConsultationMode() != null) {
                appointment.setConsultationMode(
                        request.getConsultationMode()
                );
            }

            if (request.getRemarks() != null) {
                appointment.setRemarks(
                        request.getRemarks()
                );
            }

            AppointmentEntity updatedAppointment =
                    appointmentRepo.save(appointment);

            notificationService.createNotification(
                    updatedAppointment.getPatient().getAccount(),
                    "Appointment Updated",
                    "Your appointment ("
                            + updatedAppointment.getAppointmentNumber()
                            + ") has been updated.",
                    NotificationType.APPOINTMENT_UPDATED
            );

            AppointmentResponse response =
                    appointmentMapper.toResponse(
                            updatedAppointment
                    );

            response.setMessage(
                    "Appointment updated successfully"
            );

            return response;

        } catch (AccessDeniedException e) {
            throw e;

        } catch (IllegalArgumentException e) {
            throw e;

        } catch (Exception e) {
            throw new RuntimeException(
                    "Unable to update appointment.",
                    e
            );
        }
    }

    @Override
    @Transactional
    public void deleteAppointment(UUID id) {

        try {

            if (id == null) {
                throw new IllegalArgumentException(
                        "Appointment Id is required"
                );
            }

            UUID hospitalId =
                    requireCurrentHospital();

            UserEntity currentUser =
                    SecurityUtils.getCurrentUser();

            /*
             * DB-level tenant filtering.
             */
            AppointmentEntity appointment =
                    appointmentRepo
                            .findByIdAndHospitalId(
                                    id,
                                    hospitalId
                            )
                            .orElseThrow(() -> {
                                    if (appointmentRepo.existsById(id)) {
                                        return new AccessDeniedException("Access denied");
                                    }
                                    return new IllegalArgumentException("Appointment not found");
                                });

            /*
             * Patient → own appointment only.
             */
            if (SecurityUtils.isPatient()) {

                if (appointment.getPatient() == null
                        || appointment.getPatient().getAccount() == null
                        || !appointment.getPatient()
                        .getAccount()
                        .getId()
                        .equals(currentUser.getId())) {

                    throw new AccessDeniedException(
                            "You are not authorized to cancel this appointment"
                    );
                }
            }

            /*
             * Doctor → own appointment only.
             */
            if ("DOCTOR".equalsIgnoreCase(
                    SecurityUtils.getCurrentUser().getRole().getRoleName())) {

                DoctorEntity doctor =
                        doctorService
                                .findDoctorEntityByAccountId(
                                        currentUser.getId()
                                )
                                .orElseThrow(() ->
                                        new AccessDeniedException(
                                                "Doctor profile not found"
                                        ));

                if (appointment.getDoctor() == null
                        || !appointment.getDoctor()
                        .getId()
                        .equals(doctor.getId())) {

                    throw new AccessDeniedException(
                            "Doctors can only cancel their own appointments"
                    );
                }
            }

            /*
             * Receptionist tenant validation.
             */
            if ("RECEPTIONIST".equalsIgnoreCase(
                    SecurityUtils.getCurrentUser().getRole().getRoleName())) {

                ReceptionistEntity receptionist =
                        receptionistService
                                .findReceptionistEntityByAccountId(
                                        currentUser.getId()
                                )
                                .orElseThrow(() ->
                                        new AccessDeniedException(
                                                "Receptionist profile not found"
                                        ));

                if (receptionist.getHospital() == null
                        || !hospitalId.equals(
                        receptionist.getHospital().getId())) {

                    throw new AccessDeniedException(
                            "You are not authorized to cancel appointments outside your hospital"
                    );
                }
            }

            appointment.setAppointmentStatus(
                    AppointmentEntity.AppointmentStatus.CANCELLED
            );

            appointmentRepo.save(appointment);

            notificationService.createNotification(
                    appointment.getPatient().getAccount(),
                    "Appointment Cancelled",
                    "Your appointment ("
                            + appointment.getAppointmentNumber()
                            + ") scheduled for "
                            + appointment.getAppointmentDate()
                            + " has been cancelled.",
                    NotificationType.APPOINTMENT_CANCELLED
            );

        } catch (AccessDeniedException e) {
            throw e;

        } catch (IllegalArgumentException e) {
            throw e;

        } catch (Exception e) {
            throw new RuntimeException(
                    "Unable to cancel appointment.",
                    e
            );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppointmentResponse> getTodayAppointments() {

        try {

            UUID hospitalId =
                    requireCurrentHospital();

            UserEntity currentUser =
                    SecurityUtils.getCurrentUser();

            /*
             * Doctor → own appointments + own hospital.
             */
            if ("DOCTOR".equalsIgnoreCase(
                    SecurityUtils.getCurrentUser().getRole().getRoleName())) {

                DoctorEntity doctor =
                        doctorService
                                .findDoctorEntityByAccountId(
                                        currentUser.getId()
                                )
                                .orElseThrow(() ->
                                        new AccessDeniedException(
                                                "Doctor profile not found"
                                        ));

                return appointmentRepo
                        .findByDoctorIdAndAppointmentDateAndHospitalId(
                                doctor.getId(),
                                LocalDate.now(),
                                hospitalId
                        )
                        .stream()
                        .map(appointmentMapper::toResponse)
                        .toList();
            }

            /*
             * Patient → own appointments + own hospital.
             */
            if (SecurityUtils.isPatient()) {

                PatientEntity patient =
                        patientService.findPatientByAccountId(
                                currentUser.getId()
                        );

                return appointmentRepo
                        .findByPatientIdAndHospitalId(
                                patient.getId(),
                                hospitalId
                        )
                        .stream()
                        .filter(a ->
                                LocalDate.now().equals(
                                        a.getAppointmentDate()
                                ))
                        .map(appointmentMapper::toResponse)
                        .toList();
            }

            /*
             * Receptionist/Admin → hospital scoped.
             */
            return appointmentRepo
                    .findByAppointmentDateAndHospitalId(
                            LocalDate.now(),
                            hospitalId
                    )
                    .stream()
                    .map(appointmentMapper::toResponse)
                    .toList();

        } catch (AccessDeniedException e) {
            throw e;

        } catch (Exception e) {
            throw new RuntimeException(
                    "Unable to fetch today's appointments.",
                    e
            );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppointmentResponse> getAppointmentsByDoctor(
            UUID doctorId) {

        try {

            if (doctorId == null) {
                throw new IllegalArgumentException(
                        "Doctor Id is required"
                );
            }

            UUID hospitalId =
                    requireCurrentHospital();

            UserEntity currentUser =
                    SecurityUtils.getCurrentUser();

            DoctorEntity targetDoctor = doctorService.findDoctorById(doctorId);
            if (targetDoctor.getHospital() == null || !targetDoctor.getHospital().getId().equals(hospitalId)) {
                throw new AccessDeniedException("Doctor does not belong to your hospital");
            }

            /*
             * Doctor can only request own appointments.
             */
            if ("DOCTOR".equalsIgnoreCase(
                    SecurityUtils.getCurrentUser().getRole().getRoleName())) {

                DoctorEntity currentDoctor =
                        doctorService
                                .findDoctorEntityByAccountId(
                                        currentUser.getId()
                                )
                                .orElseThrow(() ->
                                        new AccessDeniedException(
                                                "Doctor profile not found"
                                        ));

                if (!currentDoctor.getId().equals(doctorId)) {

                    throw new AccessDeniedException(
                            "You are not authorized to view another doctor's appointments"
                    );
                }
            }

            return appointmentRepo
                    .findByDoctorIdAndHospitalId(
                            doctorId,
                            hospitalId
                    )
                    .stream()
                    .map(appointmentMapper::toResponse)
                    .toList();

        } catch (AccessDeniedException e) {
            throw e;

        } catch (IllegalArgumentException e) {
            throw e;

        } catch (Exception e) {
            throw new RuntimeException(
                    "Unable to fetch doctor's appointments.",
                    e
            );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppointmentResponse> getAppointmentsByPatient(
            UUID patientId) {

        try {

            if (patientId == null) {
                throw new IllegalArgumentException(
                        "Patient Id is required"
                );
            }

            UUID hospitalId =
                    requireCurrentHospital();

            UserEntity currentUser =
                    SecurityUtils.getCurrentUser();

            PatientEntity targetPatient = patientService.findPatientById(patientId);
            if (targetPatient.getHospital() == null || !targetPatient.getHospital().getId().equals(hospitalId)) {
                throw new AccessDeniedException("Patient does not belong to your hospital");
            }

            /*
             * Patient → own appointments only.
             */
            if (SecurityUtils.isPatient()) {

                PatientEntity patient =
                        patientService.findPatientById(
                                patientId
                        );

                if (patient.getAccount() == null
                        || !patient.getAccount()
                        .getId()
                        .equals(currentUser.getId())) {

                    throw new AccessDeniedException(
                            "You are not authorized to view these appointments"
                    );
                }
            }

            /*
             * DB-level hospital filtering.
             */
            return appointmentRepo
                    .findByPatientIdAndHospitalId(
                            patientId,
                            hospitalId
                    )
                    .stream()
                    .map(appointmentMapper::toResponse)
                    .toList();

        } catch (AccessDeniedException e) {
            throw e;

        } catch (IllegalArgumentException e) {
            throw e;

        } catch (Exception e) {
            throw new RuntimeException(
                    "Unable to fetch patient's appointments.",
                    e
            );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<LocalTime> getAvailableSlots(
            UUID doctorId,
            LocalDate date) {

        try {

            if (doctorId == null || date == null) {
                throw new IllegalArgumentException(
                        "Doctor Id and Date are required"
                );
            }

            UUID hospitalId =
                    requireCurrentHospital();

            /*
             * Verify doctor belongs to current hospital.
             */
            DoctorEntity doctor =
                    doctorService.findDoctorById(doctorId);

            if (doctor.getHospital() == null
                    || !hospitalId.equals(
                    doctor.getHospital().getId())) {

                throw new AccessDeniedException(
                        "Doctor does not belong to your hospital"
                );
            }

            /*
             * Generate slots from 09:00 to 17:00.
             */
            List<LocalTime> allSlots =
                    new ArrayList<>();

            LocalTime startTime =
                    LocalTime.of(9, 0);

            LocalTime endTime =
                    LocalTime.of(17, 0);

            while (startTime.isBefore(endTime)) {

                allSlots.add(startTime);

                startTime =
                        startTime.plusMinutes(30);
            }

            /*
             * IMPORTANT:
             * Only appointments from the current hospital.
             */
            List<AppointmentEntity> existingAppointments =
                    appointmentRepo
                            .findByDoctorIdAndAppointmentDateAndHospitalId(
                                    doctorId,
                                    date,
                                    hospitalId
                            );

            List<LocalTime> bookedTimes =
                    existingAppointments
                            .stream()
                            .filter(a ->
                                    a.getAppointmentStatus()
                                            != AppointmentEntity.AppointmentStatus.CANCELLED
                            )
                            .map(
                                    AppointmentEntity::getAppointmentTime
                            )
                            .toList();

            LocalDate today =
                    LocalDate.now();

            LocalTime now =
                    LocalTime.now();

            return allSlots
                    .stream()
                    .filter(time ->
                            !bookedTimes.contains(time)
                    )
                    .filter(time -> {

                        if (date.isEqual(today)) {
                            return time.isAfter(now);
                        }

                        return date.isAfter(today);
                    })
                    .toList();

        } catch (AccessDeniedException e) {
            throw e;

        } catch (IllegalArgumentException e) {
            throw e;

        } catch (Exception e) {
            throw new RuntimeException(
                    "Unable to fetch available slots.",
                    e
            );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public AppointmentEntity findAppointmentById(
            UUID appointmentId) {

        if (appointmentId == null) {
            throw new IllegalArgumentException(
                    "Appointment Id is required"
            );
        }

        UUID hospitalId =
                requireCurrentHospital();

        return appointmentRepo
                .findByIdAndHospitalId(
                        appointmentId,
                        hospitalId
                )
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Appointment not found"
                        ));
    }

    @Override
    public void save(AppointmentEntity app) {

        if (app == null) {
            throw new IllegalArgumentException(
                    "Appointment is required"
            );
        }

        UUID hospitalId =
                requireCurrentHospital();

        if (app.getHospital() == null
                || !hospitalId.equals(
                app.getHospital().getId())) {

            throw new AccessDeniedException(
                    "Cannot save appointment outside your hospital"
            );
        }

        appointmentRepo.save(app);
    }

    private UUID requireCurrentHospital() {

        UUID hospitalId =
                tenantContextService
                        .getCurrentUserHospitalId();

        if (hospitalId == null) {
            throw new AccessDeniedException(
                    "Hospital context is required"
            );
        }

        return hospitalId;
    }

    private String generateAppointmentCode(
            LocalDate date) {

        String dateStr =
                date.toString()
                        .replace("-", "");

        String rand =
                UUID.randomUUID()
                        .toString()
                        .substring(0, 5)
                        .toUpperCase();

        return "APT-" + dateStr + "-" + rand;
    }

    @Override
    public long count() {
        return appointmentRepo.count();
    }

    @Override
    public long countByAppointmentDate(
            LocalDate date) {

        return appointmentRepo
                .countByAppointmentDate(date);
    }

    @Override
    public boolean existsByDoctorIdAndPatientId(
            UUID doctorId,
            UUID patientId
    ) {
        return appointmentRepo
                .existsByDoctorIdAndPatientId(
                        doctorId,
                        patientId
                );
    }

    @Override
    public boolean existsByDoctorIdAndPatientIdAndHospitalId(
            UUID doctorId,
            UUID patientId,
            UUID hospitalId
    ) {
        return appointmentRepo
                .existsByDoctorIdAndPatientIdAndHospitalId(
                        doctorId,
                        patientId,
                        hospitalId
                );
    }
}