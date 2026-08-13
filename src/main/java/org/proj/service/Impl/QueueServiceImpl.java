package org.proj.service.Impl;

import org.proj.dto.QueueRequest;
import org.proj.dto.QueueResponse;
import org.proj.entity.QueueEntity;
import org.proj.entity.QueueEntity.QueueStatus;
import org.proj.entity.AppointmentEntity;
import org.proj.entity.AppointmentEntity.AppointmentStatus;
import org.proj.mapper.QueueMapper;
import org.proj.repository.QueueRepo;

import org.proj.service.AppointmentService;
import org.proj.service.QueueService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class QueueServiceImpl implements QueueService {

    @Autowired
    private QueueRepo queueRepo;

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private QueueMapper queueMapper;

    @Override
    @Transactional
    public QueueResponse checkIn(QueueRequest request) {
        try {
            if (request.getAppointmentId() == null) {
                throw new IllegalArgumentException("Appointment ID is required");
            }

            AppointmentEntity appointment = appointmentService.findAppointmentById(request.getAppointmentId());

            if (appointment.getAppointmentStatus() == AppointmentStatus.CANCELLED) {
                throw new IllegalArgumentException("Cannot check in a cancelled appointment");
            }

            if (appointment.getAppointmentStatus() == AppointmentStatus.CHECKED_IN
                    || appointment.getAppointmentStatus() == AppointmentStatus.IN_PROGRESS
                    || appointment.getAppointmentStatus() == AppointmentStatus.COMPLETED) {
                throw new IllegalArgumentException("Appointment is already checked in");
            }

            if (queueRepo.existsByAppointmentId(request.getAppointmentId())) {
                throw new IllegalArgumentException("Queue entry already exists for this appointment");
            }

            appointment.setAppointmentStatus(AppointmentStatus.CHECKED_IN);
            appointmentService.save(appointment);

            LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
            long count = queueRepo.countByDoctorIdAndCheckedInTimeAfter(appointment.getDoctor().getId(), startOfToday);
            int nextQueueNumber = (int) count + 1;
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
            QueueResponse response = queueMapper.toResponse(savedQueue);
            response.setMessage("Patient checked in and queue token generated successfully");
            return response;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Unable to check in patient.", e);
        }
    }

    @Override
    public List<QueueResponse> getTodayQueue() {
        try {
            return queueRepo.findByCheckedInTimeAfter(LocalDate.now().atStartOfDay()).stream()
                    .map(queueMapper::toResponse)
                    .toList();
        } catch (Exception e) {
            throw new RuntimeException("Unable to fetch today's queue.", e);
        }
    }

    @Override
    public List<QueueResponse> getTodayQueueByDoctor(UUID doctorId) {
        try {
            if (doctorId == null) {
                throw new IllegalArgumentException("Doctor ID is required");
            }
            return queueRepo.findByDoctorIdAndCheckedInTimeAfterOrderByQueueNumberAsc(doctorId, LocalDate.now().atStartOfDay()).stream()
                    .map(queueMapper::toResponse)
                    .toList();
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Unable to fetch queue for doctor.", e);
        }
    }

    @Override
    @Transactional
    public QueueResponse callNext(UUID doctorId) {
        try {
            if (doctorId == null) {
                throw new IllegalArgumentException("Doctor ID is required");
            }

            LocalDateTime startOfToday = LocalDate.now().atStartOfDay();

            Optional<QueueEntity> calledOpt = queueRepo.findFirstByDoctorIdAndQueueStatusAndCheckedInTimeAfterOrderByQueueNumberAsc(
                    doctorId, QueueStatus.CALLED, startOfToday);
            if (calledOpt.isPresent()) {
                QueueEntity calledQueue = calledOpt.get();
                calledQueue.setQueueStatus(QueueStatus.SKIPPED);
                queueRepo.save(calledQueue);
            }

            Optional<QueueEntity> inConsultOpt = queueRepo.findFirstByDoctorIdAndQueueStatusAndCheckedInTimeAfterOrderByQueueNumberAsc(
                    doctorId, QueueStatus.IN_CONSULTATION, startOfToday);
            if (inConsultOpt.isPresent()) {
                QueueEntity consultQueue = inConsultOpt.get();
                consultQueue.setQueueStatus(QueueStatus.COMPLETED);
                consultQueue.setConsultationEnd(LocalDateTime.now());
                queueRepo.save(consultQueue);

                AppointmentEntity app = consultQueue.getAppointment();
                app.setAppointmentStatus(AppointmentStatus.COMPLETED);
                appointmentService.save(app);
            }

            QueueEntity nextQueue = queueRepo.findFirstByDoctorIdAndQueueStatusAndCheckedInTimeAfterOrderByQueueNumberAsc(
                    doctorId, QueueStatus.WAITING, startOfToday)
                    .orElseThrow(() -> new IllegalArgumentException("No patients waiting in queue"));

            nextQueue.setQueueStatus(QueueStatus.CALLED);
            QueueEntity savedQueue = queueRepo.save(nextQueue);
            QueueResponse response = queueMapper.toResponse(savedQueue);
            response.setMessage("Next patient called successfully");
            return response;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Unable to call next patient.", e);
        }
    }

    @Override
    @Transactional
    public QueueResponse startConsultation(UUID queueId) {
        try {
            if (queueId == null) {
                throw new IllegalArgumentException("Queue ID is required");
            }

            QueueEntity queue = queueRepo.findById(queueId)
                    .orElseThrow(() -> new IllegalArgumentException("Queue entry not found"));

            if (queue.getQueueStatus() != QueueStatus.CALLED) {
                throw new IllegalArgumentException("Patient must be called before starting consultation");
            }

            queue.setQueueStatus(QueueStatus.IN_CONSULTATION);
            queue.setConsultationStart(LocalDateTime.now());

            AppointmentEntity app = queue.getAppointment();
            app.setAppointmentStatus(AppointmentStatus.IN_PROGRESS);
            appointmentService.save(app);

            QueueEntity saved = queueRepo.save(queue);
            QueueResponse response = queueMapper.toResponse(saved);
            response.setMessage("Consultation started successfully");
            return response;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Unable to start consultation.", e);
        }
    }

    @Override
    @Transactional
    public QueueResponse completeConsultation(UUID queueId) {
        try {
            if (queueId == null) {
                throw new IllegalArgumentException("Queue ID is required");
            }

            QueueEntity queue = queueRepo.findById(queueId)
                    .orElseThrow(() -> new IllegalArgumentException("Queue entry not found"));

            if (queue.getQueueStatus() != QueueStatus.IN_CONSULTATION) {
                throw new IllegalArgumentException("Consultation is not in progress");
            }

            queue.setQueueStatus(QueueStatus.COMPLETED);
            queue.setConsultationEnd(LocalDateTime.now());

            AppointmentEntity app = queue.getAppointment();
            app.setAppointmentStatus(AppointmentStatus.COMPLETED);
            appointmentService.save(app);

            QueueEntity saved = queueRepo.save(queue);
            QueueResponse response = queueMapper.toResponse(saved);
            response.setMessage("Consultation completed successfully");
            return response;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Unable to complete consultation.", e);
        }
    }

    @Override
    @Transactional
    public QueueResponse skipQueue(UUID queueId) {
        try {
            if (queueId == null) {
                throw new IllegalArgumentException("Queue ID is required");
            }

            QueueEntity queue = queueRepo.findById(queueId)
                    .orElseThrow(() -> new IllegalArgumentException("Queue entry not found"));

            if (queue.getQueueStatus() != QueueStatus.CALLED && queue.getQueueStatus() != QueueStatus.WAITING) {
                throw new IllegalArgumentException("Can only skip waiting or called patients");
            }

            queue.setQueueStatus(QueueStatus.SKIPPED);

            AppointmentEntity app = queue.getAppointment();
            app.setAppointmentStatus(AppointmentStatus.NO_SHOW);
            appointmentService.save(app);

            QueueEntity saved = queueRepo.save(queue);
            QueueResponse response = queueMapper.toResponse(saved);
            response.setMessage("Patient skipped successfully");
            return response;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Unable to skip patient.", e);
        }
    }
}
