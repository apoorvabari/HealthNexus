package org.proj.repository;

import org.proj.entity.QueueEntity;
import org.proj.entity.QueueEntity.QueueStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface QueueRepo extends JpaRepository<QueueEntity, UUID> {
    Optional<QueueEntity> findByIdAndHospitalId(UUID id, UUID hospitalId);

    boolean existsByAppointmentIdAndHospitalId(UUID appointmentId, UUID hospitalId);

    long countByDoctorIdAndHospitalIdAndCheckedInTimeAfter(UUID doctorId, UUID hospitalId, LocalDateTime startOfDay);

    List<QueueEntity> findByHospitalIdAndCheckedInTimeAfter(UUID hospitalId, LocalDateTime startOfDay);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<QueueEntity> findByCheckedInTimeAfterAndQueueStatusAndQueueReminderSentAtIsNull(
            LocalDateTime startOfDay,
            QueueStatus queueStatus
    );

    List<QueueEntity> findByDoctorIdAndHospitalIdAndCheckedInTimeAfter(UUID doctorId, UUID hospitalId, LocalDateTime startOfDay);

    List<QueueEntity> findByDoctorIdAndHospitalIdAndCheckedInTimeAfterOrderByQueueNumberAsc(UUID doctorId, UUID hospitalId, LocalDateTime startOfDay);

    Optional<QueueEntity> findFirstByDoctorIdAndHospitalIdAndQueueStatusAndCheckedInTimeAfterOrderByQueueNumberAsc(
            UUID doctorId, UUID hospitalId, QueueStatus status, LocalDateTime startOfDay);

    boolean existsByAppointmentId(UUID appointmentId);

    long countByDoctorIdAndCheckedInTimeAfter(UUID doctorId, LocalDateTime startOfDay);

    List<QueueEntity> findByCheckedInTimeAfter(LocalDateTime startOfDay);

    List<QueueEntity> findByDoctorIdAndCheckedInTimeAfterOrderByQueueNumberAsc(UUID doctorId, LocalDateTime startOfDay);

    Optional<QueueEntity> findFirstByDoctorIdAndQueueStatusAndCheckedInTimeAfterOrderByQueueNumberAsc(
            UUID doctorId, QueueStatus status, LocalDateTime startOfDay);
}
