package org.proj.repository;

import org.proj.entity.PrescriptionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PrescriptionRepo extends JpaRepository<PrescriptionEntity, UUID> {
    List<PrescriptionEntity> findByPatientId(UUID patientId);
    List<PrescriptionEntity> findByDoctorId(UUID doctorId);
    List<PrescriptionEntity> findByAppointmentId(UUID appointmentId);

    Optional<PrescriptionEntity> findByIdAndAppointmentHospitalId(
            UUID id,
            UUID hospitalId
    );

    List<PrescriptionEntity> findByPatientIdAndPatientHospitalId(
            UUID patientId,
            UUID hospitalId
    );

    List<PrescriptionEntity> findByDoctorIdAndDoctorHospitalId(
            UUID doctorId,
            UUID hospitalId
    );

    List<PrescriptionEntity> findByAppointmentIdAndAppointmentHospitalId(
            UUID appointmentId,
            UUID hospitalId
    );
}
