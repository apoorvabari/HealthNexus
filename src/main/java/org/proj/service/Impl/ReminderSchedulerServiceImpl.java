package org.proj.service.impl;

import lombok.RequiredArgsConstructor;
import org.proj.entity.AppointmentEntity;
import org.proj.entity.NotificationEntity.NotificationType;
import org.proj.entity.QueueEntity;
import org.proj.entity.QueueEntity.QueueStatus;
import org.proj.repository.AppointmentRepo;
import org.proj.repository.QueueRepo;
import org.proj.service.NotificationService;
import org.proj.service.ReminderSchedulerService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * Creates idempotent in-app reminders for upcoming appointments and patients
 * who are approaching the front of today's queue.
 *
 * This is a system/background operation, so it intentionally does not depend
 * on TenantContextService. Each repository query and entity relationship is
 * evaluated explicitly, hospital by hospital through the stored entity graph.
 */
@Service
@RequiredArgsConstructor
public class ReminderSchedulerServiceImpl implements ReminderSchedulerService {

    private static final int DEFAULT_APPOINTMENT_REMINDER_MINUTES = 60;

    private final AppointmentRepo appointmentRepo;
    private final QueueRepo queueRepo;
    private final NotificationService notificationService;

    @Value("${reminder.appointment.minutes-before:60}")
    private int appointmentReminderMinutes;

    @Value("${reminder.queue.patients-ahead:2}")
    private int queuePatientsAheadLimit;

    @Value("${app.timezone:Asia/Kolkata}")
    private String appTimezone;

    @Override
    @Scheduled(
            fixedRateString = "${reminder.scheduler.fixed-rate-ms:60000}",
            initialDelayString = "${reminder.scheduler.initial-delay-ms:300000}"
    )
    @Transactional
    public void processAppointmentReminders() {
        ZoneId zoneId = ZoneId.of(appTimezone);
        LocalDateTime now = LocalDateTime.now(zoneId);
        int reminderMinutes = appointmentReminderMinutes > 0
                ? appointmentReminderMinutes
                : DEFAULT_APPOINTMENT_REMINDER_MINUTES;

        LocalDate today = now.toLocalDate();
        LocalDate tomorrow = today.plusDays(1);

        processAppointmentDate(today, now, reminderMinutes, zoneId);
        processAppointmentDate(tomorrow, now, reminderMinutes, zoneId);
    }

    private void processAppointmentDate(
            LocalDate date,
            LocalDateTime now,
            int reminderMinutes,
            ZoneId zoneId
    ) {
        List<AppointmentEntity> appointments =
                appointmentRepo.findByAppointmentDateAndAppointmentStatusAndAppointmentReminderSentAtIsNull(
                        date,
                        AppointmentEntity.AppointmentStatus.SCHEDULED
                );

        LocalDateTime reminderUpperBound =
                now.plusMinutes(reminderMinutes);

        for (AppointmentEntity appointment : appointments) {
            if (appointment.getAppointmentDate() == null
                    || appointment.getAppointmentTime() == null
                    || appointment.getPatient() == null
                    || appointment.getPatient().getAccount() == null) {
                continue;
            }

            LocalDateTime appointmentDateTime = LocalDateTime.of(
                    appointment.getAppointmentDate(),
                    appointment.getAppointmentTime()
            );

            /*
             * Reminder is sent only inside the configured window and never for a
             * past appointment. The sent timestamp makes the operation idempotent.
             */
            if (appointmentDateTime.isBefore(now)
                    || appointmentDateTime.isAfter(reminderUpperBound)) {
                continue;
            }

            long minutesRemaining = Math.max(
                    0,
                    Duration.between(now, appointmentDateTime).toMinutes()
            );

            notificationService.createNotification(
                    appointment.getPatient().getAccount(),
                    "Appointment Reminder",
                    "Reminder: your appointment "
                            + appointment.getAppointmentNumber()
                            + " is scheduled in approximately "
                            + minutesRemaining
                            + " minute(s), on "
                            + appointment.getAppointmentDate()
                            + " at "
                            + appointment.getAppointmentTime()
                            + ".",
                    NotificationType.APPOINTMENT_REMINDER
            );

            appointment.setAppointmentReminderSentAt(LocalDateTime.now(zoneId));
            appointmentRepo.save(appointment);
        }
    }

    @Override
    @Scheduled(
            fixedRateString = "${reminder.scheduler.fixed-rate-ms:60000}",
            initialDelayString = "${reminder.scheduler.initial-delay-ms:300000}"
    )
    @Transactional
    public void processQueueReminders() {
        ZoneId zoneId = ZoneId.of(appTimezone);
        LocalDateTime startOfToday = LocalDate.now(zoneId).atStartOfDay();

        int patientsAheadLimit = Math.max(0, queuePatientsAheadLimit);

        List<QueueEntity> waitingQueues =
                queueRepo.findByCheckedInTimeAfterAndQueueStatusAndQueueReminderSentAtIsNull(
                        startOfToday,
                        QueueStatus.WAITING
                );

        for (QueueEntity queue : waitingQueues) {
            if (queue.getAppointment() == null
                    || queue.getAppointment().getPatient() == null
                    || queue.getAppointment().getPatient().getAccount() == null
                    || queue.getDoctor() == null
                    || queue.getDoctor().getId() == null
                    || queue.getHospital() == null
                    || queue.getHospital().getId() == null
                    || queue.getQueueNumber() == null) {
                continue;
            }

            long patientsAhead = queueRepo
                    .findByDoctorIdAndHospitalIdAndCheckedInTimeAfterOrderByQueueNumberAsc(
                            queue.getDoctor().getId(),
                            queue.getHospital() != null ? queue.getHospital().getId() : null,
                            startOfToday
                    )
                    .stream()
                    .filter(other -> other.getQueueNumber() != null
                            && queue.getQueueNumber() != null
                            && other.getQueueNumber() < queue.getQueueNumber())
                    .filter(other -> other.getQueueStatus() == QueueStatus.WAITING
                            || other.getQueueStatus() == QueueStatus.CALLED
                            || other.getQueueStatus() == QueueStatus.IN_CONSULTATION)
                    .count();

            if (patientsAhead > patientsAheadLimit) {
                continue;
            }

            notificationService.createNotification(
                    queue.getAppointment().getPatient().getAccount(),
                    "Queue Reminder",
                    "Your queue token "
                            + queue.getTokenNumber()
                            + " is approaching. There are approximately "
                            + patientsAhead
                            + " patient(s) ahead of you.",
                    NotificationType.QUEUE_REMINDER
            );

            queue.setQueueReminderSentAt(LocalDateTime.now(zoneId));
            queueRepo.save(queue);
        }
    }
}
