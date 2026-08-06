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
}
