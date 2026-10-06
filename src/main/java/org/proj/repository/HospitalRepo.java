package org.proj.repository;

import org.proj.entity.HospitalEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface HospitalRepo extends JpaRepository<HospitalEntity, UUID> {

    Optional<HospitalEntity> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByHospitalCode(String hospitalCode);

    boolean existsByRegistrationNumber(String registrationNumber);

    Optional<HospitalEntity> findByHospitalCode(String hospitalCode);

    @Query("SELECT MAX(h.hospitalCode) FROM HospitalEntity h")
    String findMaxHospitalCode();

    long countByVerificationStatus(
            HospitalEntity.VerificationStatus verificationStatus
    );

    long countById(UUID hospitalId);

    @Query("""
            SELECT h
            FROM HospitalEntity h
            WHERE h.status = org.proj.entity.HospitalEntity$HospitalStatus.ACTIVE
              AND h.verificationStatus = org.proj.entity.HospitalEntity$VerificationStatus.APPROVED
            ORDER BY LOWER(h.hospitalName) ASC
            """)
    java.util.List<HospitalEntity> findPublicHospitals();

    @Query("""
            SELECT h
            FROM HospitalEntity h
            WHERE h.id = :id
              AND h.status = org.proj.entity.HospitalEntity$HospitalStatus.ACTIVE
              AND h.verificationStatus = org.proj.entity.HospitalEntity$VerificationStatus.APPROVED
            """)
    Optional<HospitalEntity> findPublicHospitalById(@Param("id") UUID id);

    @Query("""
            SELECT h
            FROM HospitalEntity h
            WHERE h.status = org.proj.entity.HospitalEntity$HospitalStatus.ACTIVE
              AND h.verificationStatus = org.proj.entity.HospitalEntity$VerificationStatus.APPROVED
              AND (
                  :search IS NULL
                  OR :search = ''
                  OR LOWER(h.hospitalName) LIKE LOWER(CONCAT('%', :search, '%'))
                  OR LOWER(h.hospitalCode) LIKE LOWER(CONCAT('%', :search, '%'))
                  OR LOWER(h.email) LIKE LOWER(CONCAT('%', :search, '%'))
              )
            ORDER BY LOWER(h.hospitalName) ASC
            """)
    Page<HospitalEntity> searchPublicHospitals(
            @Param("search") String search,
            Pageable pageable
    );

    long countByIdAndVerificationStatus(
            UUID hospitalId,
            HospitalEntity.VerificationStatus status
    );

    @Query("""
            SELECT h
            FROM HospitalEntity h
            WHERE h.id = :hospitalId
            """)
    Optional<HospitalEntity> findByIdForTenant(
            UUID hospitalId
    );

    @Query("""
            SELECT h
            FROM HospitalEntity h
            WHERE h.id = :hospitalId
            AND (
                :search IS NULL
                OR :search = ''
                OR LOWER(h.hospitalName) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(h.hospitalCode) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(h.email) LIKE LOWER(CONCAT('%', :search, '%'))
            )
            """)
    Page<HospitalEntity> searchHospitalsForTenant(
            UUID hospitalId,
            String search,
            Pageable pageable
    );

    @Query("""
            SELECT h
            FROM HospitalEntity h
            JOIN AdminEntity a
                ON a.hospital.id = h.id
            WHERE a.account.id = :accountId
            AND a.status = org.proj.entity.AdminEntity$AdminStatus.ACTIVE
            AND (
                :search IS NULL
                OR :search = ''
                OR LOWER(h.hospitalName) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(h.hospitalCode) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(h.email) LIKE LOWER(CONCAT('%', :search, '%'))
            )
            """)
    Page<HospitalEntity> searchHospitalsByAdmin(
            UUID accountId,
            String search,
            Pageable pageable
    );
}
