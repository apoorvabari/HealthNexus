package org.proj.service.impl;

import org.proj.entity.ConsultationEntity;
import org.proj.entity.ConsultationEntity.ConsultationStatus;
import org.proj.dto.QueueRequest;
import org.proj.dto.QueueResponse;
import org.proj.entity.AppointmentEntity;
import org.proj.entity.AppointmentEntity.AppointmentStatus;
import org.proj.entity.QueueEntity;
import org.proj.entity.QueueEntity.QueueStatus;
import org.proj.mapper.QueueMapper;
import org.proj.service.NotificationService;
import org.proj.repository.QueueRepo;
import org.proj.service.AppointmentService;
import org.proj.service.DoctorService;
import org.proj.service.ReceptionistService;
import org.proj.service.QueueService;
import org.proj.service.TenantContextService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import org.proj.security.SecurityUtils;

import org.proj.entity.NotificationEntity;
import org.proj.entity.ReceptionistEntity;
import org.proj.entity.DoctorEntity;
import org.proj.entity.UserEntity;

import org.proj.repository.ConsultationRepo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.Optional;

@Service
public class QueueServiceImpl implements QueueService {

    @Autowired
    private QueueRepo queueRepo;

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private ConsultationRepo consultationRepo;

    @Autowired
    private QueueMapper queueMapper;

    @Autowired
    private TenantContextService tenantContextService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private ReceptionistService receptionistService;

    @Autowired
    private DoctorService doctorService;

    @Override
    @Transactional
    public QueueResponse checkIn(QueueRequest request) {
        try {
            QueueRequest validReq = Optional.ofNullable(request)
                    .filter(r -> r.getAppointmentId() != null)
                    .orElseThrow(() -> new IllegalArgumentException("Appointment ID is required"));

            UUID hospitalId = requireCurrentHospital();

            AppointmentEntity appointment = Optional.ofNullable(appointmentService.findAppointmentById(validReq.getAppointmentId()))
                    .orElseThrow(() -> new IllegalArgumentException("Appointment not found"));

            UUID apptHospitalId = (appointment.getHospital() != null) ? appointment.getHospital().getId() : null;
            if (apptHospitalId == null || !apptHospitalId.equals(hospitalId)) {
                throw new AccessDeniedException("Appointment does not belong to your hospital");
            }

            if (appointment.getAppointmentStatus() == AppointmentStatus.CANCELLED) {
                throw new IllegalArgumentException("Cannot check in a cancelled appointment");
            }

            if (appointment.getAppointmentStatus() == AppointmentStatus.CHECKED_IN
                    || appointment.getAppointmentStatus() == AppointmentStatus.IN_PROGRESS
                    || appointment.getAppointmentStatus() == AppointmentStatus.COMPLETED) {
                throw new IllegalArgumentException("Appointment is already checked in");
            }

            if (queueRepo.existsByAppointmentIdAndHospitalId(validReq.getAppointmentId(), hospitalId)) {
                throw new IllegalArgumentException("Queue entry already exists for this appointment");
            }

            appointment.setAppointmentStatus(AppointmentStatus.CHECKED_IN);
            appointmentService.save(appointment);

            LocalDateTime startOfToday = LocalDate.now().atStartOfDay();

            UUID doctorId = (appointment.getDoctor() != null) ? appointment.getDoctor().getId() : null;
            if (doctorId == null) {
                throw new IllegalArgumentException("Appointment doctor is required");
            }

            doctorService.findLockedByIdAndHospitalId(doctorId, hospitalId)
                    .orElseThrow(() -> new AccessDeniedException("Doctor does not belong to your hospital"));

            long count = queueRepo.countByDoctorIdAndHospitalIdAndCheckedInTimeAfter(doctorId, hospitalId, startOfToday);
            int nextQueueNumber = Math.toIntExact(count + 1);
            String nextTokenNumber = String.format("Q%03d", nextQueueNumber);

            QueueEntity queue = queueMapper.toEntity(
                    nextQueueNumber,
                    nextTokenNumber,
                    appointment.getHospital(),
                    appointment.getDepartment(),
                    appointment.getDoctor(),
                    appointment
            );

            QueueEntity savedQueue = queueRepo.save(queue);

            String patientAccountName = (appointment.getPatient() != null && appointment.getPatient().getAccountName() != null)
                    ? appointment.getPatient().getAccountName()
                    : "";

            if (appointment.getPatient() != null && appointment.getPatient().getAccount() != null) {
                notificationService.createNotification(
                        appointment.getPatient().getAccount(),
                        "Check-in Successful",
                        "Check-in successful! Your queue token is " + savedQueue.getTokenNumber() + ".",
                        NotificationEntity.NotificationType.QUEUE_REMINDER
                );
            }

            if (appointment.getDoctor() != null && appointment.getDoctor().getAccount() != null) {
                notificationService.createNotification(
                        appointment.getDoctor().getAccount(),
                        "Patient Checked In",
                        "Patient " + patientAccountName + " has checked in. Token " + savedQueue.getTokenNumber() + " is waiting in queue.",
                        NotificationEntity.NotificationType.QUEUE_REMINDER
                );
            }

            if (appointment.getHospital() != null && appointment.getHospital().getId() != null) {
                List<ReceptionistEntity> receptionists = receptionistService.findReceptionistsByHospitalId(appointment.getHospital().getId());
                if (receptionists != null) {
                    for (ReceptionistEntity rec : receptionists) {
                        if (rec != null && rec.getAccount() != null) {
                            notificationService.createNotification(
                                    rec.getAccount(),
                                    "Patient Checked In",
                                    "Patient " + patientAccountName + " checked in. Token " + savedQueue.getTokenNumber() + " is active.",
                                    NotificationEntity.NotificationType.QUEUE_REMINDER
                            );
                        }
                    }
                }
            }

            QueueResponse response = queueMapper.toResponse(savedQueue);
            response.setMessage("Patient checked in and queue token generated successfully");
            return response;
        } catch (AccessDeniedException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            String msg = e.getMessage() != null && !e.getMessage().isBlank()
                    ? e.getMessage()
                    : "Unable to check in patient.";
            throw new RuntimeException(msg, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<QueueResponse> getTodayQueue() {
        try {
            UUID hospitalId = requireCurrentHospital();
            return queueRepo.findByHospitalIdAndCheckedInTimeAfter(hospitalId, LocalDate.now().atStartOfDay())
                    .stream()
                    .map(queueMapper::toResponse)
                    .toList();
        } catch (AccessDeniedException e) {
            throw e;
        } catch (Exception e) {
            String msg = e.getMessage() != null && !e.getMessage().isBlank()
                    ? e.getMessage()
                    : "Unable to fetch today's queue.";
            throw new RuntimeException(msg, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<QueueResponse> getTodayQueueByDoctor(UUID doctorId) {
        try {
            UUID validDoctorId = Optional.ofNullable(doctorId)
                    .orElseThrow(() -> new IllegalArgumentException("Doctor ID is required"));

            UUID hospitalId = requireCurrentHospital();

            return queueRepo.findByDoctorIdAndHospitalIdAndQueueStatusInAndCheckedInTimeAfterOrderByQueueNumberAsc(
                    validDoctorId,
                    hospitalId,
                    List.of(QueueStatus.WAITING, QueueStatus.CALLED, QueueStatus.IN_CONSULTATION),
                    LocalDate.now().atStartOfDay()
            ).stream()
            .map(queueMapper::toResponse)
            .toList();
        } catch (AccessDeniedException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            String msg = e.getMessage() != null && !e.getMessage().isBlank()
                    ? e.getMessage()
                    : "Unable to fetch queue for doctor.";
            throw new RuntimeException(msg, e);
        }
    }

    @Override
    @Transactional
    public QueueResponse callNext(UUID doctorId) {
        try {
            UUID validDoctorId = Optional.ofNullable(doctorId)
                    .orElseThrow(() -> new IllegalArgumentException("Doctor ID is required"));

            UUID hospitalId = requireCurrentHospital();
            LocalDateTime startOfToday = LocalDate.now().atStartOfDay();

            Optional<QueueEntity> calledOpt = queueRepo.findFirstByDoctorIdAndHospitalIdAndQueueStatusAndCheckedInTimeAfterOrderByQueueNumberAsc(
                    validDoctorId,
                    hospitalId,
                    QueueStatus.CALLED,
                    startOfToday
            );
            if (calledOpt.isPresent()) {
                throw new IllegalArgumentException("Current patient is already called. Start the consultation or explicitly skip the patient before calling the next patient.");
            }

            Optional<QueueEntity> inConsultOpt = queueRepo.findFirstByDoctorIdAndHospitalIdAndQueueStatusAndCheckedInTimeAfterOrderByQueueNumberAsc(
                    validDoctorId,
                    hospitalId,
                    QueueStatus.IN_CONSULTATION,
                    startOfToday
            );
            if (inConsultOpt.isPresent()) {
                throw new IllegalArgumentException("A patient is currently in consultation. Please complete the clinical visit summary before calling the next patient.");
            }

            QueueEntity nextQueue = queueRepo.findFirstByDoctorIdAndHospitalIdAndQueueStatusAndCheckedInTimeAfterOrderByQueueNumberAsc(
                    validDoctorId,
                    hospitalId,
                    QueueStatus.WAITING,
                    startOfToday
            ).orElseThrow(() -> new IllegalArgumentException("No patients waiting in queue"));

            nextQueue.setQueueStatus(QueueStatus.CALLED);
            QueueEntity savedQueue = queueRepo.save(nextQueue);

            QueueResponse response = queueMapper.toResponse(savedQueue);
            response.setMessage("Next patient called successfully");
            return response;
        } catch (AccessDeniedException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            String msg = e.getMessage() != null && !e.getMessage().isBlank()
                    ? e.getMessage()
                    : "Unable to call next patient.";
            throw new RuntimeException(msg, e);
        }
    }

    @Override
    @Transactional
    public QueueResponse startConsultation(UUID queueId) {
        try {
            UUID validQueueId = Optional.ofNullable(queueId)
                    .orElseThrow(() -> new IllegalArgumentException("Queue ID is required"));

            QueueEntity queue = findAndValidateQueue(validQueueId);

            if (queue.getQueueStatus() == QueueStatus.IN_CONSULTATION) {
                QueueResponse response = queueMapper.toResponse(queue);
                response.setMessage("Consultation is already in progress");
                return response;
            }

            Optional.ofNullable(queue.getQueueStatus())
                    .filter(status -> status == QueueStatus.CALLED || status == QueueStatus.WAITING)
                    .orElseThrow(() -> new IllegalArgumentException("Patient must be called or waiting before starting consultation"));

            queue.setQueueStatus(QueueStatus.IN_CONSULTATION);
            queue.setConsultationStart(LocalDateTime.now());

            AppointmentEntity app = queue.getAppointment();
            if (app != null) {
                app.setAppointmentStatus(AppointmentStatus.IN_PROGRESS);
                appointmentService.save(app);

                UUID appHospitalId = (queue.getHospital() != null) ? queue.getHospital().getId() : null;
                ConsultationEntity consultation = (appHospitalId != null && app.getId() != null)
                        ? consultationRepo.findByAppointmentIdAndAppointmentHospitalId(app.getId(), appHospitalId).orElse(null)
                        : null;

                if (consultation == null) {
                    ConsultationEntity newConsultation = ConsultationEntity.builder()
                            .appointment(app)
                            .doctor(app.getDoctor())
                            .patient(app.getPatient())
                            .status(ConsultationStatus.STARTED)
                            .startTime(LocalDateTime.now())
                            .build();
                    consultationRepo.save(newConsultation);
                }
            }

            QueueEntity saved = queueRepo.save(queue);
            QueueResponse response = queueMapper.toResponse(saved);
            response.setMessage("Consultation started successfully");
            return response;
        } catch (AccessDeniedException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            String msg = e.getMessage() != null && !e.getMessage().isBlank()
                    ? e.getMessage()
                    : "Unable to start consultation.";
            throw new RuntimeException(msg, e);
        }
    }

    @Override
    @Transactional
    public QueueResponse completeConsultation(UUID queueId) {
        try {
            UUID validQueueId = Optional.ofNullable(queueId)
                    .orElseThrow(() -> new IllegalArgumentException("Queue ID is required"));

            QueueEntity queue = findAndValidateQueue(validQueueId);

            if (queue.getQueueStatus() == QueueStatus.COMPLETED) {
                QueueResponse response = queueMapper.toResponse(queue);
                response.setMessage("Consultation is already completed");
                return response;
            }

            queue.setQueueStatus(QueueStatus.COMPLETED);
            queue.setConsultationEnd(LocalDateTime.now());

            AppointmentEntity app = queue.getAppointment();
            if (app != null && app.getId() != null) {
                UUID appHospitalId = (queue.getHospital() != null) ? queue.getHospital().getId() : null;
                if (appHospitalId != null) {
                    consultationRepo.findByAppointmentIdAndAppointmentHospitalId(app.getId(), appHospitalId)
                            .ifPresent(consultation -> {
                                consultation.setStatus(ConsultationStatus.COMPLETED);
                                consultation.setEndTime(LocalDateTime.now());
                                consultationRepo.save(consultation);
                            });
                }

                app.setAppointmentStatus(AppointmentStatus.COMPLETED);
                appointmentService.save(app);
            }

            QueueEntity saved = queueRepo.save(queue);

            UserEntity patientAcc = (saved.getAppointment() != null && saved.getAppointment().getPatient() != null)
                    ? saved.getAppointment().getPatient().getAccount()
                    : null;

            if (patientAcc != null) {
                DoctorEntity doc = saved.getDoctor();
                UserEntity docAcc = (doc != null) ? doc.getAccount() : null;
                String docName = (docAcc != null && docAcc.getAccountName() != null) ? docAcc.getAccountName() : "";

                notificationService.createNotification(
                        patientAcc,
                        "Consultation Completed",
                        "Your consultation with Dr. " + docName + " has been completed.",
                        NotificationEntity.NotificationType.CONSULTATION_COMPLETED
                );
            }

            QueueResponse response = queueMapper.toResponse(saved);
            response.setMessage("Consultation completed successfully");
            return response;
        } catch (AccessDeniedException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            String msg = e.getMessage() != null && !e.getMessage().isBlank()
                    ? e.getMessage()
                    : "Unable to complete consultation.";
            throw new RuntimeException(msg, e);
        }
    }

    @Override
    @Transactional
    public QueueResponse skipQueue(UUID queueId) {
        try {
            UUID validQueueId = Optional.ofNullable(queueId)
                    .orElseThrow(() -> new IllegalArgumentException("Queue ID is required"));

            QueueEntity queue = findAndValidateQueue(validQueueId);

            Optional.ofNullable(queue.getQueueStatus())
                    .filter(status -> status == QueueStatus.CALLED || status == QueueStatus.WAITING)
                    .orElseThrow(() -> new IllegalArgumentException("Can only skip waiting or called patients"));

            queue.setQueueStatus(QueueStatus.SKIPPED);

            Optional.ofNullable(queue.getAppointment()).ifPresent(app -> {
                app.setAppointmentStatus(AppointmentStatus.NO_SHOW);
                appointmentService.save(app);
            });

            QueueEntity saved = queueRepo.save(queue);
            QueueResponse response = queueMapper.toResponse(saved);
            response.setMessage("Patient skipped successfully");
            return response;
        } catch (AccessDeniedException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            String msg = e.getMessage() != null && !e.getMessage().isBlank()
                    ? e.getMessage()
                    : "Unable to skip patient.";
            throw new RuntimeException(msg, e);
        }
    }

    @Override
    @Transactional
    public QueueResponse recallSkippedPatient(UUID queueId) {
        try {
            UUID validQueueId = Optional.ofNullable(queueId)
                    .orElseThrow(() -> new IllegalArgumentException("Queue ID is required"));

            UUID hospitalId = requireCurrentHospital();

            QueueEntity queue = queueRepo.findByIdAndHospitalId(validQueueId, hospitalId)
                    .orElseThrow(() -> new IllegalArgumentException("Queue entry not found"));

            Optional.ofNullable(queue.getQueueStatus())
                    .filter(QueueStatus.SKIPPED::equals)
                    .orElseThrow(() -> new IllegalArgumentException("Only a skipped patient can be recalled"));

            AppointmentEntity appointment = queue.getAppointment();
            if (appointment == null) {
                throw new IllegalArgumentException("Queue entry has no appointment");
            }

            UUID apptHospitalId = (appointment.getHospital() != null) ? appointment.getHospital().getId() : null;
            if (apptHospitalId == null || !apptHospitalId.equals(hospitalId)) {
                throw new AccessDeniedException("Appointment does not belong to your hospital");
            }

            if (appointment.getAppointmentStatus() != AppointmentStatus.NO_SHOW) {
                throw new IllegalArgumentException("Only a no-show appointment can be recalled from a skipped queue");
            }

            if (appointment.getAppointmentDate() == null || !appointment.getAppointmentDate().equals(LocalDate.now())) {
                throw new IllegalArgumentException("Only today's skipped patient can be recalled");
            }

            LocalDateTime startOfToday = LocalDate.now().atStartOfDay();

            long existingQueueCount = queueRepo.countByDoctorIdAndHospitalIdAndCheckedInTimeAfter(
                    queue.getDoctor().getId(),
                    hospitalId,
                    startOfToday
            );

            int nextQueueNumber = Math.toIntExact(existingQueueCount + 1);

            queue.setQueueNumber(nextQueueNumber);
            queue.setTokenNumber(String.format("Q%03d", nextQueueNumber));
            queue.setQueueStatus(QueueStatus.WAITING);
            queue.setCheckedInTime(LocalDateTime.now());
            queue.setConsultationStart(null);
            queue.setConsultationEnd(null);
            queue.setQueueReminderSentAt(null);

            appointment.setAppointmentStatus(AppointmentStatus.CHECKED_IN);
            appointmentService.save(appointment);

            QueueEntity savedQueue = queueRepo.save(queue);
            QueueResponse response = queueMapper.toResponse(savedQueue);
            response.setMessage("Skipped patient recalled successfully and returned to the waiting queue");
            return response;
        } catch (AccessDeniedException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            String msg = e.getMessage() != null && !e.getMessage().isBlank()
                    ? e.getMessage()
                    : "Unable to recall skipped patient.";
            throw new RuntimeException(msg, e);
        }
    }

    @Override
    @Transactional
    public QueueResponse updateQueueStatus(UUID queueId, String status) {
        if (status == null || status.trim().isBlank()) {
            throw new IllegalArgumentException("Status is required");
        }
        String upper = status.trim().toUpperCase(java.util.Locale.ROOT);

        return switch (upper) {
            case "IN_CONSULTATION" -> startConsultation(queueId);
            case "COMPLETED" -> completeConsultation(queueId);
            case "SKIPPED" -> skipQueue(queueId);
            case "CALLED" -> {
                QueueEntity q = findAndValidateQueue(queueId);
                q.setQueueStatus(QueueStatus.CALLED);
                yield queueMapper.toResponse(queueRepo.save(q));
            }
            case "WAITING" -> {
                QueueEntity q = findAndValidateQueue(queueId);
                q.setQueueStatus(QueueStatus.WAITING);
                yield queueMapper.toResponse(queueRepo.save(q));
            }
            default -> throw new IllegalArgumentException("Unsupported queue status: " + status);
        };
    }

    private QueueEntity findAndValidateQueue(UUID queueId) {
        if (queueId == null) {
            throw new IllegalArgumentException("Queue ID is required");
        }

        QueueEntity queue = queueRepo.findById(queueId)
                .orElseThrow(() -> new IllegalArgumentException("Queue entry not found"));

        UUID hospitalId = tenantContextService.getCurrentUserHospitalId();
        org.proj.entity.UserEntity currentUser = SecurityUtils.getCurrentUser();
        if (currentUser == null) {
            throw new AccessDeniedException("User is not authenticated");
        }

        String role = (currentUser.getRole() != null && currentUser.getRole().getRoleName() != null)
                ? currentUser.getRole().getRoleName().trim().toUpperCase(Locale.ROOT)
                : "";

        if ("DOCTOR".equals(role)) {
            DoctorEntity doctorEntity = doctorService.findDoctorEntityByAccountId(currentUser.getId()).orElse(null);
            UUID currentDoctorId = (doctorEntity != null) ? doctorEntity.getId() : null;

            boolean isAssignedDoctor = (queue.getDoctor() != null && currentDoctorId != null && queue.getDoctor().getId().equals(currentDoctorId))
                    || (queue.getDoctor() != null && queue.getDoctor().getAccount() != null && queue.getDoctor().getAccount().getId().equals(currentUser.getId()));
            boolean isSameHospital = hospitalId != null
                    && queue.getHospital() != null
                    && queue.getHospital().getId().equals(hospitalId);

            if (!isAssignedDoctor && !isSameHospital) {
                throw new AccessDeniedException("You are not authorized to manage this queue entry");
            }
        } else if (hospitalId != null && queue.getHospital() != null && !queue.getHospital().getId().equals(hospitalId)) {
            throw new AccessDeniedException("Queue entry belongs to another hospital");
        }

        return queue;
    }

    private UUID requireCurrentHospital() {
        UUID hospitalId = tenantContextService.getCurrentUserHospitalId();
        if (hospitalId == null) {
            throw new AccessDeniedException("Hospital context is required");
        }
        return hospitalId;
    }
}
