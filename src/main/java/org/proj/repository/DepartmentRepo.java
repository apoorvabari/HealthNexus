package org.proj.repository;

import org.proj.entity.DepartmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface DepartmentRepo extends JpaRepository<DepartmentEntity, UUID> {

    java.util.Optional<DepartmentEntity> findByDepartmentCode(String departmentCode);

    boolean existsByDepartmentCodeAndHospitalId(String departmentCode, UUID hospitalId);

    boolean existsByDepartmentCodeAndHospitalIdAndIdNot(String departmentCode, UUID hospitalId, UUID id);

    @org.springframework.data.jpa.repository.Query("SELECT d FROM DepartmentEntity d WHERE " +
           "(:search IS NULL OR :search = '' OR LOWER(d.departmentName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(d.departmentCode) LIKE LOWER(CONCAT('%', :search, '%')))")
    org.springframework.data.domain.Page<DepartmentEntity> searchDepartments(@org.springframework.data.repository.query.Param("search") String search, org.springframework.data.domain.Pageable pageable);
}
