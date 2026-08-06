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

    @Query("SELECT MAX(h.hospitalCode) FROM HospitalEntity h")
    String findMaxHospitalCode();
}
