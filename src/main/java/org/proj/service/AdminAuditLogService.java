package org.proj.service;

import org.proj.entity.AdminAuditLogEntity;

public interface AdminAuditLogService {

    void save(AdminAuditLogEntity log);

    java.util.List<AdminAuditLogEntity> findAll();
}
