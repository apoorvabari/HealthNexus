package org.proj.service;

import org.proj.entity.DoctorAffiliationRequestEntity;
import org.proj.entity.DoctorAffiliationRequestEntity.RequestStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DoctorAffiliationRequestService {

    DoctorAffiliationRequestEntity save(DoctorAffiliationRequestEntity request);

    Optional<DoctorAffiliationRequestEntity> findById(UUID id);

    boolean existsByDoctorIdAndTargetHospitalIdAndStatus(UUID doctorId, UUID targetHospitalId, RequestStatus status);

    List<DoctorAffiliationRequestEntity> findByDoctorIdOrderByCreatedAtDesc(UUID doctorId);

    List<DoctorAffiliationRequestEntity> findByTargetHospitalIdOrderByCreatedAtDesc(UUID hospitalId);

    List<DoctorAffiliationRequestEntity> findByTargetHospitalIdAndStatusOrderByCreatedAtDesc(UUID hospitalId, RequestStatus status);

    List<DoctorAffiliationRequestEntity> findByTargetHospitalIds(List<UUID> hospitalIds);

    List<DoctorAffiliationRequestEntity> findByTargetHospitalIdsAndStatus(List<UUID> hospitalIds, RequestStatus status);
}
