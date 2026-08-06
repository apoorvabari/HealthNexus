package org.proj.repository;

import org.proj.entity.QueueEntity;
import org.proj.entity.QueueEntity.QueueStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface QueueRepo extends JpaRepository<QueueEntity, UUID> {

    boolean existsByAppointmentId(UUID appointmentId);

    long countByDoctorIdAndCheckedInTimeAfter(UUID doctorId, LocalDateTime startOfDay);

    List<QueueEntity> findByCheckedInTimeAfter(LocalDateTime startOfDay);

    List<QueueEntity> findByDoctorIdAndCheckedInTimeAfter(UUID doctorId, LocalDateTime startOfDay);

    List<QueueEntity> findByDoctorIdAndCheckedInTimeAfterOrderByQueueNumberAsc(UUID doctorId, LocalDateTime startOfDay);

    Optional<QueueEntity> findFirstByDoctorIdAndQueueStatusAndCheckedInTimeAfterOrderByQueueNumberAsc(
            UUID doctorId, QueueStatus status, LocalDateTime startOfDay);
}
