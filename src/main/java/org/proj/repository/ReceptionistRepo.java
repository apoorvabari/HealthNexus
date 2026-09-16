package org.proj.repository;

import org.proj.entity.ReceptionistEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReceptionistRepo extends JpaRepository<ReceptionistEntity, UUID> {

    List<ReceptionistEntity> findByHospitalId(UUID hospitalId);

    Optional<ReceptionistEntity> findByIdAndHospitalId(UUID id, UUID hospitalId);

    boolean existsByEmployeeCodeAndHospitalId(String employeeCode, UUID hospitalId);

    boolean existsByEmployeeCodeAndHospitalIdAndIdNot(String employeeCode, UUID hospitalId, UUID id);

    boolean existsByAccountId(UUID accountId);

    boolean existsByAccountIdAndIdNot(UUID accountId, UUID id);

    java.util.Optional<ReceptionistEntity> findByAccountId(UUID accountId);
}
