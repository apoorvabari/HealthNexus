package org.proj.service;

import org.proj.entity.UserEntity;

import java.util.List;
import java.util.UUID;

public interface TenantContextService {

    /**
     * Resolves the active hospital for the authenticated user.
     *
     * ADMIN:
     * - One hospital  -> that hospital is used automatically.
     * - Multiple hospitals -> X-Hospital-Id must be supplied.
     *
     * DOCTOR / RECEPTIONIST / PATIENT:
     * - Their own hospital is resolved automatically.
     *
     * Returns null when the tenant cannot be resolved safely.
     */
    UUID getCurrentUserHospitalId();

    /**
     * Resolves a user's hospital.
     *
     * For ADMIN this returns the first assignment only.
     * It must NOT be used for deciding whether an ADMIN
     * can access an arbitrary hospital.
     */
    UUID getHospitalIdForUser(UserEntity user);

    /**
     * Returns all hospitals accessible by the
     * authenticated user.
     *
     * ADMIN -> all hospitals owned/assigned to that admin.
     * Other roles -> their single hospital.
     */
    List<UUID> getCurrentUserHospitalIds();

    /**
     * Server-side tenant authorization check.
     */
    boolean hasCurrentUserHospitalAccess(UUID hospitalId);
}