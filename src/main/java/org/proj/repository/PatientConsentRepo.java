package org.proj.repository;

import org.proj.entity.PatientConsentEntity;
import org.proj.entity.PatientConsentEntity.ConsentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;

@Repository
public interface PatientConsentRepo extends JpaRepository<PatientConsentEntity, UUID> {

    Optional<PatientConsentEntity> findByPatientIdAndConsentType(UUID patientId, ConsentType consentType);

    Optional<PatientConsentEntity> findByPatientIdAndConsentTypeAndDoctorIsNull(UUID patientId, ConsentType consentType);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PatientConsentEntity p WHERE p.patient.id = :patientId AND p.consentType = :consentType AND p.doctor IS NULL")
    Optional<PatientConsentEntity> findByPatientIdAndConsentTypeAndDoctorIsNullForUpdate(UUID patientId, ConsentType consentType);

    Optional<PatientConsentEntity> findByPatientIdAndConsentTypeAndDoctorId(UUID patientId, ConsentType consentType, UUID doctorId);

    Optional<PatientConsentEntity> findFirstByPatientIdAndConsentTypeAndDoctorId(UUID patientId, ConsentType consentType, UUID doctorId);

    List<PatientConsentEntity> findByPatientIdAndDoctorId(UUID patientId, UUID doctorId);

    List<PatientConsentEntity> findAllByPatientIdAndConsentType(UUID patientId, ConsentType consentType);

    List<PatientConsentEntity> findByPatientIdAndConsentTypeAndStatus(UUID patientId, ConsentType consentType, PatientConsentEntity.ConsentStatus status);

    List<PatientConsentEntity> findByPatientIdOrderByCreatedAtDesc(UUID patientId);
}
