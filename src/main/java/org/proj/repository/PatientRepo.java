package org.proj.repository;

import org.proj.entity.PatientEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PatientRepo extends JpaRepository<PatientEntity, UUID> {

    boolean existsByPatientCodeAndHospitalId(String patientCode, UUID hospitalId);

    boolean existsByPatientCodeAndHospitalIdAndIdNot(String patientCode, UUID hospitalId, UUID id);

    boolean existsByAccountId(UUID accountId);

    boolean existsByAccountIdAndIdNot(UUID accountId, UUID id);

    java.util.Optional<PatientEntity> findByAccountId(UUID accountId);
}
