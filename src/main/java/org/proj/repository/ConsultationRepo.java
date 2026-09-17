package org.proj.repository;

import org.proj.entity.ConsultationEntity;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConsultationRepo extends JpaRepository<ConsultationEntity, UUID> {

    Optional<ConsultationEntity> findByIdAndAppointmentHospitalId(
            UUID id,
            UUID hospitalId
    );

    Optional<ConsultationEntity> findByAppointmentIdAndAppointmentHospitalId(
            UUID appointmentId,
            UUID hospitalId
    );

    Optional<ConsultationEntity> findByAppointmentId(UUID appointmentId);

    List<ConsultationEntity> findByPatientIdAndStatusOrderByStartTimeAsc(
            UUID patientId,
            ConsultationEntity.ConsultationStatus status
    );

    List<ConsultationEntity> findByPatientIdAndDoctorIdAndStatusOrderByStartTimeAsc(
            UUID patientId,
            UUID doctorId,
            ConsultationEntity.ConsultationStatus status
    );
}
