package org.proj.repository;

import org.proj.entity.MedicalRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MedicalRecordRepo extends JpaRepository<MedicalRecordEntity, UUID> {

    List<MedicalRecordEntity> findByPatientId(UUID patientId);
    List<MedicalRecordEntity> findByDoctorId(UUID doctorId);
    List<MedicalRecordEntity> findByAppointmentId(UUID appointmentId);

    Optional<MedicalRecordEntity> findByIdAndPatientHospitalId(
            UUID id,
            UUID hospitalId
    );

    List<MedicalRecordEntity> findByPatientIdAndPatientHospitalId(
            UUID patientId,
            UUID hospitalId
    );

    List<MedicalRecordEntity> findByDoctorIdAndDoctorHospitalId(
            UUID doctorId,
            UUID hospitalId
    );

    List<MedicalRecordEntity> findByAppointmentIdAndAppointmentHospitalId(
            UUID appointmentId,
            UUID hospitalId
    );
}
