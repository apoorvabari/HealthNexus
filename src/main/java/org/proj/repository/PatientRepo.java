package org.proj.repository;

import org.proj.entity.PatientEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PatientRepo extends JpaRepository<PatientEntity, UUID> {

    List<PatientEntity> findByHospitalId(UUID hospitalId);

    long countByHospitalId(UUID hospitalId);

    Optional<PatientEntity> findByIdAndHospitalId(
            UUID id,
            UUID hospitalId
    );

    boolean existsByPatientCodeAndHospitalId(
            String patientCode,
            UUID hospitalId
    );

    boolean existsByPatientCodeAndHospitalIdAndIdNot(
            String patientCode,
            UUID hospitalId,
            UUID id
    );

    boolean existsByAccountId(UUID accountId);

    boolean existsByAccountIdAndIdNot(
            UUID accountId,
            UUID id
    );

    Optional<PatientEntity> findByAccountId(UUID accountId);

    Optional<PatientEntity> findByAccountIdAndHospitalId(
            UUID accountId,
            UUID hospitalId
    );
}
