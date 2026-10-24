package org.proj.repository;

import org.proj.entity.DoctorAffiliationRequestEntity;
import org.proj.entity.DoctorAffiliationRequestEntity.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DoctorAffiliationRequestRepo extends JpaRepository<DoctorAffiliationRequestEntity, UUID> {

    List<DoctorAffiliationRequestEntity> findByDoctorIdOrderByCreatedAtDesc(UUID doctorId);

    List<DoctorAffiliationRequestEntity> findByTargetHospitalIdOrderByCreatedAtDesc(UUID hospitalId);

    List<DoctorAffiliationRequestEntity> findByTargetHospitalIdAndStatusOrderByCreatedAtDesc(UUID hospitalId, RequestStatus status);

    boolean existsByDoctorIdAndTargetHospitalIdAndStatus(UUID doctorId, UUID targetHospitalId, RequestStatus status);

    @Query("SELECT r FROM DoctorAffiliationRequestEntity r " +
           "WHERE r.targetHospital.id IN :hospitalIds " +
           "ORDER BY r.createdAt DESC")
    List<DoctorAffiliationRequestEntity> findByTargetHospitalIds(@Param("hospitalIds") List<UUID> hospitalIds);

    @Query("SELECT r FROM DoctorAffiliationRequestEntity r " +
           "WHERE r.targetHospital.id IN :hospitalIds AND r.status = :status " +
           "ORDER BY r.createdAt DESC")
    List<DoctorAffiliationRequestEntity> findByTargetHospitalIdsAndStatus(
            @Param("hospitalIds") List<UUID> hospitalIds,
            @Param("status") RequestStatus status);
}
