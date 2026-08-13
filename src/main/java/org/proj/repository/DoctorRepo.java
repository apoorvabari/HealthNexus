package org.proj.repository;

import org.proj.entity.DoctorEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface DoctorRepo extends JpaRepository<DoctorEntity, UUID> {

    boolean existsByLicenseNumber(String licenseNumber);

    boolean existsByLicenseNumberAndIdNot(String licenseNumber, UUID id);

    boolean existsByAccountId(UUID accountId);

    boolean existsByAccountIdAndIdNot(UUID accountId, UUID id);

    java.util.Optional<DoctorEntity> findByAccountId(UUID accountId);

    @org.springframework.data.jpa.repository.Query("SELECT d FROM DoctorEntity d WHERE " +
           "(:search IS NULL OR :search = '' OR LOWER(d.account.firstName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(d.account.lastName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(d.specialization) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(d.licenseNumber) LIKE LOWER(CONCAT('%', :search, '%')))")
    org.springframework.data.domain.Page<DoctorEntity> searchDoctors(@org.springframework.data.repository.query.Param("search") String search, org.springframework.data.domain.Pageable pageable);
}
