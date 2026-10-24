package org.proj.repository;

import org.proj.entity.DoctorHospitalAffiliationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DoctorHospitalAffiliationRepo extends JpaRepository<DoctorHospitalAffiliationEntity, UUID> {

    List<DoctorHospitalAffiliationEntity> findByDoctorId(UUID doctorId);

    List<DoctorHospitalAffiliationEntity> findByHospitalId(UUID hospitalId);

    Optional<DoctorHospitalAffiliationEntity> findByDoctorIdAndHospitalId(UUID doctorId, UUID hospitalId);

    Optional<DoctorHospitalAffiliationEntity> findByDoctorIdAndActiveDutyTrue(UUID doctorId);

    // Tenant boundary check: ensures Doctor and Hospital both share the same Admin
    @Query("SELECT CASE WHEN COUNT(a1) > 0 THEN true ELSE false END " +
           "FROM AdminEntity a1, AdminEntity a2 " +
           "WHERE a1.hospital.id = :hospitalId AND a2.account.id = :doctorAccountId AND a1.account.id = a2.account.id")
    boolean isDoctorAndHospitalUnderSameAdmin(@Param("doctorAccountId") UUID doctorAccountId, @Param("hospitalId") UUID hospitalId);

    @Modifying
    @Query("UPDATE DoctorHospitalAffiliationEntity a SET a.activeDuty = false WHERE a.doctor.id = :doctorId")
    void resetActiveDutyForDoctor(@Param("doctorId") UUID doctorId);
}
