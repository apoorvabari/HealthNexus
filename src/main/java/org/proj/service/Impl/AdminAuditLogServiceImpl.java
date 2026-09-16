package org.proj.service.impl;

import lombok.RequiredArgsConstructor;
import org.proj.entity.AdminAuditLogEntity;
import org.proj.repository.AdminAuditLogRepo;
import org.proj.service.AdminAuditLogService;
import org.proj.service.TenantContextService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminAuditLogServiceImpl implements AdminAuditLogService {

    private final AdminAuditLogRepo adminAuditLogRepo;
    private final TenantContextService tenantContextService;

    @Override
    @Transactional
    public void save(AdminAuditLogEntity log) {

        if (log == null) {
            throw new IllegalArgumentException("Audit log cannot be null");
        }

        UUID hospitalId = requireCurrentHospitalId();

        if (log.getHospital() == null
                || log.getHospital().getId() == null
                || !hospitalId.equals(log.getHospital().getId())) {

            throw new AccessDeniedException(
                    "Audit log hospital does not match current hospital"
            );
        }

        adminAuditLogRepo.save(log);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminAuditLogEntity> findAll() {

        UUID hospitalId = requireCurrentHospitalId();

        return adminAuditLogRepo.findAllByHospitalId(hospitalId);
    }

    private UUID requireCurrentHospitalId() {

        UUID hospitalId =
                tenantContextService.getCurrentUserHospitalId();

        if (hospitalId == null) {
            throw new AccessDeniedException(
                    "Hospital context is required"
            );
        }

        return hospitalId;
    }
}