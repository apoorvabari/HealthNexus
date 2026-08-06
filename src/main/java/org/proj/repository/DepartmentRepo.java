package org.proj.repository;

import org.proj.entity.DepartmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface DepartmentRepo extends JpaRepository<DepartmentEntity, UUID> {

    boolean existsByDepartmentCodeAndHospitalId(String departmentCode, UUID hospitalId);

    boolean existsByDepartmentCodeAndHospitalIdAndIdNot(String departmentCode, UUID hospitalId, UUID id);
}
