package org.proj.repository;

import org.proj.entity.AdminAuditLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AdminAuditLogRepo extends JpaRepository<AdminAuditLogEntity, UUID> {

    @org.springframework.data.jpa.repository.Query("SELECT a FROM AdminAuditLogEntity a WHERE " +
           "(:search IS NULL OR :search = '' OR LOWER(a.action) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(a.entityName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(a.details) LIKE LOWER(CONCAT('%', :search, '%')))")
    org.springframework.data.domain.Page<AdminAuditLogEntity> searchLogs(@org.springframework.data.repository.query.Param("search") String search, org.springframework.data.domain.Pageable pageable);
}
