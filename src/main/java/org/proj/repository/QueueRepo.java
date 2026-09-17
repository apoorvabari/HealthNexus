package org.proj.repository;

import org.proj.entity.QueueEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface QueueRepo extends JpaRepository<QueueEntity, UUID> {

    boolean existsByAppointmentIdAndHospitalId(UUID appointmentId, UUID hospitalId);

    long countByDoctorIdAndHospitalIdAndCheckedInTimeAfter(UUID doctorId, UUID hospitalId, LocalDateTime checkedInTime);

    List<QueueEntity> findByHospitalIdAndCheckedInTimeAfter(UUID hospitalId, LocalDateTime checkedInTime);

    List<QueueEntity> findByDoctorIdAndHospitalIdAndCheckedInTimeAfterOrderByQueueNumberAsc(UUID doctorId, UUID hospitalId, LocalDateTime checkedInTime);

    Optional<QueueEntity> findFirstByDoctorIdAndHospitalIdAndQueueStatusAndCheckedInTimeAfterOrderByQueueNumberAsc(UUID doctorId, UUID hospitalId, QueueEntity.QueueStatus queueStatus, LocalDateTime checkedInTime);

    Optional<QueueEntity> findByIdAndHospitalId(UUID id, UUID hospitalId);

    List<QueueEntity> findByCheckedInTimeAfterAndQueueStatusAndQueueReminderSentAtIsNull(LocalDateTime checkedInTime, QueueEntity.QueueStatus queueStatus);
}
