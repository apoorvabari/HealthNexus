package org.proj.repository;

import org.proj.entity.DoctorEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import java.util.Optional;

import java.util.UUID;

@Repository
public interface DoctorRepo extends JpaRepository<DoctorEntity, UUID> {

    boolean existsByLicenseNumber(String licenseNumber);

    boolean existsByLicenseNumberAndIdNot(String licenseNumber, UUID id);

    boolean existsByAccountId(UUID accountId);

    boolean existsByAccountIdAndIdNot(UUID accountId, UUID id);

    Optional<DoctorEntity> findByAccountId(UUID accountId);

    long countByVerificationStatus(
        DoctorEntity.VerificationStatus verificationStatus
);

    @Query("SELECT d FROM DoctorEntity d WHERE " +
           "(:search IS NULL OR :search = '' OR LOWER(d.account.firstName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(d.account.lastName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(d.specialization) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(d.licenseNumber) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<DoctorEntity> searchDoctors(String search, Pageable pageable);
}
