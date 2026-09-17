package org.proj.repository;

import org.proj.entity.DepartmentEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DepartmentRepo extends JpaRepository<DepartmentEntity, UUID> {

    Optional<DepartmentEntity> findByDepartmentCode(String departmentCode);

    boolean existsByDepartmentCodeAndHospitalId(
            String departmentCode,
            UUID hospitalId);

    boolean existsByDepartmentCodeAndHospitalIdAndIdNot(
            String departmentCode,
            UUID hospitalId,
            UUID id);

    Optional<DepartmentEntity> findByIdAndHospitalId(
            UUID id,
            UUID hospitalId);

    @Query("""
            SELECT d
            FROM DepartmentEntity d
            WHERE d.hospital.id = :hospitalId
              AND d.status != 'INACTIVE'
              AND (
                    :search IS NULL
                    OR :search = ''
                    OR LOWER(d.departmentName) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR LOWER(d.departmentCode) LIKE LOWER(CONCAT('%', :search, '%'))
                  )
            """)
    Page<DepartmentEntity> searchDepartmentsByHospital(
            @Param("search") String search,
            @Param("hospitalId") UUID hospitalId,
            Pageable pageable);
}
