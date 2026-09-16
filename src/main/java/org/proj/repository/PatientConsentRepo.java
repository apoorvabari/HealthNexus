package org.proj.repository;

import org.proj.entity.PatientConsentEntity;
import org.proj.entity.PatientConsentEntity.ConsentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PatientConsentRepo extends JpaRepository<PatientConsentEntity, UUID> {

    Optional<PatientConsentEntity> findByPatientIdAndConsentType(UUID patientId, ConsentType consentType);

    List<PatientConsentEntity> findByPatientIdOrderByCreatedAtDesc(UUID patientId);
}
