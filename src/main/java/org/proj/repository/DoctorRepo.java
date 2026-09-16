package org.proj.repository;

import org.proj.entity.DoctorEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DoctorRepo extends JpaRepository<DoctorEntity, UUID> {

        List<DoctorEntity> findByHospitalId(UUID hospitalId);

        Optional<DoctorEntity> findByIdAndHospitalId(UUID id, UUID hospitalId);

        @Lock(LockModeType.PESSIMISTIC_WRITE)
        @Query("SELECT d FROM DoctorEntity d WHERE d.id = :id AND d.hospital.id = :hospitalId")
        Optional<DoctorEntity> findLockedByIdAndHospitalId(@Param("id") UUID id, @Param("hospitalId") UUID hospitalId);

        boolean existsByLicenseNumber(String licenseNumber);

        boolean existsByLicenseNumberAndIdNot(String licenseNumber, UUID id);

        boolean existsByAccountId(UUID accountId);

        boolean existsByAccountIdAndIdNot(UUID accountId, UUID id);

        Optional<DoctorEntity> findByAccountId(UUID accountId);

        Optional<DoctorEntity> findByAccountEmail(String email);

        long countByVerificationStatus(
                        DoctorEntity.VerificationStatus verificationStatus);

        long countByHospitalId(UUID hospitalId);

        long countByHospitalIdAndVerificationStatus(
                        UUID hospitalId,
                        DoctorEntity.VerificationStatus verificationStatus);

        long countByDepartmentId(UUID departmentId);

        long countByDepartmentIdAndStatus(
                        UUID departmentId,
                        DoctorEntity.DoctorStatus status);

        Page<DoctorEntity> findByDepartmentId(
                        UUID departmentId,
                        Pageable pageable);

        Page<DoctorEntity> findByDepartmentIdAndHospitalId(
                        UUID departmentId,
                        UUID hospitalId,
                        Pageable pageable);

        long countByDepartmentIdAndHospitalId(
                        UUID departmentId,
                        UUID hospitalId);

        long countByDepartmentIdAndHospitalIdAndStatus(
                        UUID departmentId,
        UUID hospitalId,
        DoctorEntity.DoctorStatus status);

        @Query("SELECT d FROM DoctorEntity d WHERE " +
                        "(:search IS NULL OR :search = '' OR LOWER(d.account.firstName) LIKE LOWER(CONCAT('%', :search, '%')) "
                        +
                        "OR LOWER(d.account.lastName) LIKE LOWER(CONCAT('%', :search, '%')) " +
                        "OR LOWER(d.specialization) LIKE LOWER(CONCAT('%', :search, '%')) " +
                        "OR LOWER(d.licenseNumber) LIKE LOWER(CONCAT('%', :search, '%')))")
        Page<DoctorEntity> searchDoctors(@Param("search") String search, Pageable pageable);

        @Query("SELECT d FROM DoctorEntity d WHERE " +
                        "d.hospital.id = :hospitalId AND " +
                        "(:search IS NULL OR :search = '' OR LOWER(d.account.firstName) LIKE LOWER(CONCAT('%', :search, '%')) " +
                        "OR LOWER(d.account.lastName) LIKE LOWER(CONCAT('%', :search, '%')) " +
                        "OR LOWER(d.specialization) LIKE LOWER(CONCAT('%', :search, '%')) " +
                        "OR LOWER(d.licenseNumber) LIKE LOWER(CONCAT('%', :search, '%')))")
        Page<DoctorEntity> searchDoctorsByHospital(@Param("search") String search, @Param("hospitalId") UUID hospitalId, Pageable pageable);

        @Query("SELECT d FROM DoctorEntity d WHERE " +
                        "d.hospital.id = :hospitalId AND " +
                        "d.verificationStatus = 'APPROVED' AND d.status = 'ACTIVE' AND " +
                        "(:search IS NULL OR :search = '' OR LOWER(d.account.firstName) LIKE LOWER(CONCAT('%', :search, '%')) " +
                        "OR LOWER(d.account.lastName) LIKE LOWER(CONCAT('%', :search, '%')) " +
                        "OR LOWER(d.specialization) LIKE LOWER(CONCAT('%', :search, '%')) " +
                        "OR LOWER(d.licenseNumber) LIKE LOWER(CONCAT('%', :search, '%')))")
        Page<DoctorEntity> searchApprovedAndActiveDoctorsByHospital(@Param("search") String search, @Param("hospitalId") UUID hospitalId, Pageable pageable);

        @Query("SELECT d FROM DoctorEntity d WHERE " +
                        "d.verificationStatus = 'APPROVED' AND d.status = 'ACTIVE' AND " +
                        "(:search IS NULL OR :search = '' OR LOWER(d.account.firstName) LIKE LOWER(CONCAT('%', :search, '%')) "
                        +
                        "OR LOWER(d.account.lastName) LIKE LOWER(CONCAT('%', :search, '%')) " +
                        "OR LOWER(d.specialization) LIKE LOWER(CONCAT('%', :search, '%')) " +
                        "OR LOWER(d.licenseNumber) LIKE LOWER(CONCAT('%', :search, '%')))")
        Page<DoctorEntity> searchApprovedAndActiveDoctors(@Param("search") String search, Pageable pageable);

        @Query("""
                        SELECT d
                        FROM DoctorEntity d
                        WHERE d.verificationStatus = 'APPROVED'
                          AND d.status = 'ACTIVE'

                          AND (
                              :search IS NULL
                              OR :search = ''
                              OR LOWER(d.account.firstName) LIKE LOWER(CONCAT('%', :search, '%'))
                              OR LOWER(d.account.lastName) LIKE LOWER(CONCAT('%', :search, '%'))
                              OR LOWER(d.specialization) LIKE LOWER(CONCAT('%', :search, '%'))
                              OR LOWER(d.qualification) LIKE LOWER(CONCAT('%', :search, '%'))
                          )

                          AND (
                              :specialization IS NULL
                              OR :specialization = ''
                              OR LOWER(d.specialization) = LOWER(:specialization)
                          )

                          AND (
                              :hospitalId IS NULL
                              OR d.hospital.id = :hospitalId
                          )

                          AND (
                              :departmentId IS NULL
                              OR d.department.id = :departmentId
                          )

                          AND (
                              :maxFee IS NULL
                              OR d.consultationFee <= :maxFee
                          )

                          AND (
                              :minExperience IS NULL
                              OR d.experience >= :minExperience
                          )
                        """)
        Page<DoctorEntity> searchDoctorsForPatient(
                        @Param("search") String search,
                        @Param("specialization") String specialization,
                        @Param("hospitalId") UUID hospitalId,
                        @Param("departmentId") UUID departmentId,
                        @Param("maxFee") BigDecimal maxFee,
                        @Param("minExperience") Integer minExperience,
                        Pageable pageable);

}
