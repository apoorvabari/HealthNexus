package org.proj.service;

import org.proj.entity.AdminAuditLogEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface AdminAuditLogService {

    void save(AdminAuditLogEntity log);

    java.util.List<AdminAuditLogEntity> findAll();

    Page<AdminAuditLogEntity> searchLogs(UUID hospitalId, String search, Pageable pageable);
}
