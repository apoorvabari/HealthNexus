package org.proj.service.impl;

import lombok.RequiredArgsConstructor;
import org.proj.dto.AppointmentRequest;
import org.proj.dto.ConsultationRequest;
import org.proj.dto.ConsultationResponse;
import org.proj.entity.AppointmentEntity;
import org.proj.entity.ConsultationEntity;
import org.proj.entity.UserEntity;
import org.proj.entity.NotificationEntity.NotificationType;
import org.proj.repository.ConsultationRepo;
import org.proj.security.SecurityUtils;
import org.proj.service.AppointmentService;
import org.proj.service.ConsultationService;
import org.proj.service.MedicalRecordService;
import org.proj.service.NotificationService;
import org.proj.service.TenantContextService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ConsultationServiceImpl implements ConsultationService {

    private final ConsultationRepo consultationRepo;
    private final MedicalRecordService medicalRecordService;
    private final AppointmentService appointmentService;
    private final NotificationService notificationService;
    private final TenantContextService tenantContextService;

    @Override
    @Transactional
    public ConsultationResponse startConsultation(ConsultationRequest request) {

        if (request == null || request.getAppointmentId() == null) {
            throw new IllegalArgumentException("Appointment Id is required");
        }

        UUID hospitalId = requireCurrentHospital();

        /*
         * AppointmentService.findAppointmentById() is already
         * tenant-scoped in the current architecture.
         */
        AppointmentEntity appointment =
                appointmentService.findAppointmentById(
                        request.getAppointmentId()
                );

        validateAppointmentTenant(appointment, hospitalId);
        validateAppointmentForConsultation(appointment);

        ConsultationEntity existing =
                consultationRepo
                        .findByAppointmentIdAndAppointmentHospitalId(
                                appointment.getId(),
                                hospitalId
                        )
                        .orElse(null);

        if (existing != null) {
            throw new IllegalArgumentException(
                    "Consultation already started for this appointment"
            );
        }

        /*
         * Only the assigned doctor or an admin of the same hospital
         * can start the consultation.
         */
        validateDoctorAccessForAppointment(
                appointment,
                hospitalId
        );

        AppointmentRequest updateAppointmentRequest =
                new AppointmentRequest();

        updateAppointmentRequest.setAppointmentStatus(
                AppointmentEntity.AppointmentStatus.IN_PROGRESS
        );

        appointmentService.updateAppointment(
                appointment.getId(),
                updateAppointmentRequest
        );

        ConsultationEntity consultation =
                ConsultationEntity.builder()
                        .appointment(appointment)
                        .doctor(appointment.getDoctor())
                        .patient(appointment.getPatient())
                        .remarks(request.getRemarks())
                        .status(
                                ConsultationEntity.ConsultationStatus.STARTED
                        )
                        .startTime(LocalDateTime.now())
                        .build();

        ConsultationEntity saved =
                consultationRepo.save(consultation);

        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public ConsultationResponse saveVisit(
            UUID consultationId,
            String remarks) {

        UUID hospitalId = requireCurrentHospital();

        ConsultationEntity consultation =
                consultationRepo
                        .findByIdAndAppointmentHospitalId(
                                consultationId,
                                hospitalId
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Consultation not found"
                                ));

        validateDoctorAccess(
                consultation,
                hospitalId
        );

        if (consultation.getStatus()
                != ConsultationEntity.ConsultationStatus.STARTED) {

            throw new IllegalArgumentException(
                    "Visit can only be saved while the consultation is in progress"
            );
        }

        if (remarks == null || remarks.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Visit summary is required"
            );
        }

        if (medicalRecordService.getMedicalRecordByAppointment(
                consultation.getAppointment().getId()
        ) == null) {

            throw new IllegalArgumentException(
                    "Medical record must be saved before saving the visit"
            );
        }

        consultation.setRemarks(remarks.trim());
        consultation.setVisitSaved(true);

        ConsultationEntity saved =
                consultationRepo.save(consultation);

        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public ConsultationResponse completeConsultation(
            UUID consultationId,
            String remarks) {

        UUID hospitalId = requireCurrentHospital();

        ConsultationEntity consultation =
                consultationRepo
                        .findByIdAndAppointmentHospitalId(
                                consultationId,
                                hospitalId
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Consultation not found"
                                ));

        validateDoctorAccess(
                consultation,
                hospitalId
        );

        if (consultation.getStatus()
                == ConsultationEntity.ConsultationStatus.COMPLETED) {

            throw new IllegalArgumentException(
                    "Consultation is already completed"
            );
        }

        if (!consultation.isVisitSaved()) {
            throw new IllegalArgumentException(
                    "Save the visit before completing the consultation"
            );
        }

        consultation.setStatus(
                ConsultationEntity.ConsultationStatus.COMPLETED
        );

        consultation.setEndTime(
                LocalDateTime.now()
        );

        if (remarks != null && !remarks.trim().isEmpty()) {
            consultation.setRemarks(remarks.trim());
        }

        ConsultationEntity saved =
                consultationRepo.save(consultation);

        AppointmentRequest updateAppointmentRequest =
                new AppointmentRequest();

        updateAppointmentRequest.setAppointmentStatus(
                AppointmentEntity.AppointmentStatus.COMPLETED
        );

        appointmentService.updateAppointment(
                consultation.getAppointment().getId(),
                updateAppointmentRequest
        );

        notificationService.createNotification(
                consultation.getPatient().getAccount(),
                "Consultation Completed",
                "Your consultation with "
                        + consultation.getDoctor()
                        .getAccount()
                        .getFirstName()
                        + " has been completed.",
                NotificationType.CONSULTATION_COMPLETED
        );

        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ConsultationResponse getConsultationById(UUID id) {

        UUID hospitalId = requireCurrentHospital();

        ConsultationEntity consultation =
                consultationRepo
                        .findByIdAndAppointmentHospitalId(
                                id,
                                hospitalId
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Consultation not found"
                                ));

        validateAccess(
                consultation,
                hospitalId
        );

        return mapToResponse(consultation);
    }

    @Override
    @Transactional(readOnly = true)
    public ConsultationResponse getConsultationByAppointmentId(
            UUID appointmentId) {

        UUID hospitalId = requireCurrentHospital();

        ConsultationEntity consultation =
                consultationRepo
                        .findByAppointmentIdAndAppointmentHospitalId(
                                appointmentId,
                                hospitalId
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Consultation not found for appointment"
                                ));

        validateAccess(
                consultation,
                hospitalId
        );

        return mapToResponse(consultation);
    }

    private UUID requireCurrentHospital() {

        UUID hospitalId =
                tenantContextService.getCurrentUserHospitalId();

        if (hospitalId == null) {
            throw new AccessDeniedException(
                    "Tenant hospital context is required"
            );
        }

        return hospitalId;
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
                    "You are not authorized to access an appointment from another hospital"
            );
        }

        if (appointment.getDoctor() == null
                || appointment.getDoctor().getHospital() == null
                || !hospitalId.equals(
                        appointment.getDoctor().getHospital().getId())) {

            throw new AccessDeniedException(
                    "Appointment doctor does not belong to the current hospital"
            );
        }

        if (appointment.getPatient() == null
                || appointment.getPatient().getHospital() == null
                || !hospitalId.equals(
                        appointment.getPatient().getHospital().getId())) {

            throw new AccessDeniedException(
                    "Appointment patient does not belong to the current hospital"
            );
        }

        if (appointment.getDepartment() == null
                || appointment.getDepartment().getHospital() == null
                || !hospitalId.equals(
                        appointment.getDepartment().getHospital().getId())) {

            throw new AccessDeniedException(
                    "Appointment department does not belong to the current hospital"
            );
        }
    }

    private void validateDoctorAccessForAppointment(
            AppointmentEntity appointment,
            UUID hospitalId) {

        UserEntity currentUser =
                requireCurrentUser();

        String role =
                currentUser.getRole() != null
                        ? currentUser.getRole()
                        .getRoleName()
                        .toUpperCase()
                        : "";

        if ("ADMIN".equals(role)) {

            validateAppointmentTenant(
                    appointment,
                    hospitalId
            );

            return;
        }

        if ("DOCTOR".equals(role)) {

            if (appointment.getDoctor() == null
                    || appointment.getDoctor().getAccount() == null
                    || !currentUser.getId().equals(
                            appointment.getDoctor()
                                    .getAccount()
                                    .getId())) {

                throw new AccessDeniedException(
                        "You are not authorized to perform actions on this consultation"
                );
            }

            return;
        }

        throw new AccessDeniedException(
                "Only an assigned doctor or admin can perform actions on this consultation"
        );
    }

    private void validateDoctorAccess(
            ConsultationEntity consultation,
            UUID hospitalId) {

        UserEntity currentUser =
                requireCurrentUser();

        String role =
                currentUser.getRole() != null
                        ? currentUser.getRole()
                        .getRoleName()
                        .toUpperCase()
                        : "";

        AppointmentEntity appointment =
                consultation.getAppointment();

        validateAppointmentTenant(
                appointment,
                hospitalId
        );

        if ("ADMIN".equals(role)) {
            return;
        }

        if ("DOCTOR".equals(role)) {

            if (consultation.getDoctor() == null
                    || consultation.getDoctor().getAccount() == null
                    || !currentUser.getId().equals(
                            consultation.getDoctor()
                                    .getAccount()
                                    .getId())) {

                throw new AccessDeniedException(
                        "You are not authorized to perform actions on this consultation"
                );
            }

            return;
        }

        throw new AccessDeniedException(
                "Only an assigned doctor or admin can perform actions on this consultation"
        );
    }

    private void validateAccess(
            ConsultationEntity consultation,
            UUID hospitalId) {

        UserEntity currentUser =
                requireCurrentUser();

        String role =
                currentUser.getRole() != null
                        ? currentUser.getRole()
                        .getRoleName()
                        .toUpperCase()
                        : "";

        validateAppointmentTenant(
                consultation.getAppointment(),
                hospitalId
        );

        if ("ADMIN".equals(role)
                || "RECEPTIONIST".equals(role)) {

            return;
        }

        if ("DOCTOR".equals(role)) {

            if (consultation.getDoctor() != null
                    && consultation.getDoctor().getAccount() != null
                    && currentUser.getId().equals(
                            consultation.getDoctor()
                                    .getAccount()
                                    .getId())) {

                return;
            }
        }

        if ("PATIENT".equals(role)) {

            if (consultation.getPatient() != null
                    && consultation.getPatient().getAccount() != null
                    && currentUser.getId().equals(
                            consultation.getPatient()
                                    .getAccount()
                                    .getId())) {

                return;
            }
        }

        throw new AccessDeniedException(
                "You are not authorized to view this consultation"
        );
    }

    private UserEntity requireCurrentUser() {

        UserEntity currentUser =
                SecurityUtils.getCurrentUser();

        if (currentUser == null) {
            throw new AccessDeniedException(
                    "Unauthenticated user"
            );
        }

        return currentUser;
    }

    private ConsultationResponse mapToResponse(
            ConsultationEntity entity) {

        String doctorName =
                entity.getDoctor()
                        .getAccount()
                        .getFirstName()
                        + " "
                        + entity.getDoctor()
                        .getAccount()
                        .getLastName();

        String patientName =
                entity.getPatient()
                        .getAccount()
                        .getFirstName()
                        + " "
                        + entity.getPatient()
                        .getAccount()
                        .getLastName();

        return ConsultationResponse.builder()
                .id(entity.getId())
                .appointmentId(entity.getAppointment().getId())
                .doctorId(entity.getDoctor().getId())
                .doctorName(doctorName.trim())
                .patientId(entity.getPatient().getId())
                .patientName(patientName.trim())
                .status(entity.getStatus().name())
                .startTime(entity.getStartTime())
                .endTime(entity.getEndTime())
                .remarks(entity.getRemarks())
                .visitSaved(entity.isVisitSaved())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private void validateAppointmentForConsultation(
            AppointmentEntity appointment) {

        AppointmentEntity.AppointmentStatus status =
                appointment.getAppointmentStatus();

        AppointmentEntity.AppointmentType type =
                appointment.getAppointmentType();

        if (status == null) {
            throw new IllegalArgumentException(
                    "Appointment status is not available"
            );
        }

        if (type == null) {
            throw new IllegalArgumentException(
                    "Appointment type is not available"
            );
        }

        if (status == AppointmentEntity.AppointmentStatus.CANCELLED) {
            throw new IllegalArgumentException(
                    "Cancelled appointment cannot start a consultation"
            );
        }

        if (status == AppointmentEntity.AppointmentStatus.COMPLETED) {
            throw new IllegalArgumentException(
                    "Completed appointment cannot start another consultation"
            );
        }

        if (status == AppointmentEntity.AppointmentStatus.NO_SHOW) {
            throw new IllegalArgumentException(
                    "No-show appointment cannot start a consultation"
            );
        }

        if (status == AppointmentEntity.AppointmentStatus.IN_PROGRESS) {
            throw new IllegalArgumentException(
                    "Consultation is already in progress for this appointment"
            );
        }

        if (type == AppointmentEntity.AppointmentType.WALK_IN
                && status != AppointmentEntity.AppointmentStatus.CHECKED_IN) {

            throw new IllegalArgumentException(
                    "Walk-in appointment must be checked in before starting consultation"
            );
        }

        if ((type == AppointmentEntity.AppointmentType.ONLINE
                || type == AppointmentEntity.AppointmentType.FOLLOW_UP)
                && status != AppointmentEntity.AppointmentStatus.SCHEDULED) {

            throw new IllegalArgumentException(
                    "Online or follow-up consultation must be scheduled before starting"
            );
        }
    }
}