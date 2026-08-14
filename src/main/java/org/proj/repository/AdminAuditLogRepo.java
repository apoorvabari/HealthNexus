package org.proj.repository;

import org.proj.entity.AdminAuditLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AdminAuditLogRepo extends JpaRepository<AdminAuditLogEntity, UUID> {

    @Query("SELECT a FROM AdminAuditLogEntity a WHERE " +
           "(:search IS NULL OR :search = '' OR LOWER(a.action) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(a.entityName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(a.details) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<AdminAuditLogEntity> searchLogs(String search, Pageable pageable);
}
