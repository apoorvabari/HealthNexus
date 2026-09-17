package org.proj.service;

import org.proj.entity.UserEntity;

import java.util.List;
import java.util.UUID;

public interface TenantContextService {

    UUID getCurrentUserHospitalId();

    UUID getHospitalIdForUser(UserEntity user);

    List<UUID> getCurrentUserHospitalIds();

    boolean hasCurrentUserHospitalAccess(UUID hospitalId);
}
