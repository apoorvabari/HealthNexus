package org.proj.repository;

import org.proj.entity.HospitalEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
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

    @Query("SELECT h FROM HospitalEntity h WHERE " +
           "(:search IS NULL OR :search = '' OR LOWER(h.hospitalName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(h.hospitalCode) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(h.email) LIKE LOWER(CONCAT('%', :search, '%')))")
    org.springframework.data.domain.Page<HospitalEntity> searchHospitals(@org.springframework.data.repository.query.Param("search") String search, org.springframework.data.domain.Pageable pageable);
}
