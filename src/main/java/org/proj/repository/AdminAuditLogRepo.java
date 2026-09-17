package org.proj.repository;

import org.proj.entity.AdminAuditLogEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AdminAuditLogRepo extends JpaRepository<AdminAuditLogEntity, UUID> {

    @Query("""
            SELECT a
            FROM AdminAuditLogEntity a
            WHERE a.hospital.id = :hospitalId
            AND (
                :search IS NULL
                OR :search = ''
                OR LOWER(a.action) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(a.entityName) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(a.details) LIKE LOWER(CONCAT('%', :search, '%'))
            )
            """)
    Page<AdminAuditLogEntity> searchLogs(
            @Param("hospitalId") UUID hospitalId,
            @Param("search") String search,
            Pageable pageable
    );

    @Query("""
            SELECT a
            FROM AdminAuditLogEntity a
            WHERE (:search IS NULL
                OR :search = ''
                OR LOWER(a.action) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(a.entityName) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(a.details) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<AdminAuditLogEntity> searchLogs(
            @Param("search") String search,
            Pageable pageable
    );

    List<AdminAuditLogEntity> findAllByHospitalId(UUID hospitalId);
}
