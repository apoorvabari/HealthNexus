package org.proj.repository;

import org.proj.entity.MedicalReportEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MedicalReportRepo extends JpaRepository<MedicalReportEntity, UUID> {
    List<MedicalReportEntity> findByPatientIdOrderByCreatedAtDesc(UUID patientId);

    Optional<MedicalReportEntity> findByIdAndPatientHospitalId(
            UUID id,
            UUID hospitalId
    );

    List<MedicalReportEntity> findByPatientIdAndPatientHospitalIdOrderByCreatedAtDesc(
            UUID patientId,
            UUID hospitalId
    );
}
