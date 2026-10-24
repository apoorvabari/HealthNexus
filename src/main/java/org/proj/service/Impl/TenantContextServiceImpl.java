package org.proj.service.impl;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

import org.proj.entity.UserEntity;
import org.proj.repository.AdminRepo;
import org.proj.repository.DoctorRepo;
import org.proj.repository.PatientRepo;
import org.proj.repository.ReceptionistRepo;
import org.proj.security.SecurityUtils;
import org.proj.service.TenantContextService;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

import org.proj.entity.AdminEntity;
import org.proj.entity.DoctorEntity;
import org.proj.entity.PatientEntity;
import org.proj.entity.ReceptionistEntity;

@Service
@RequiredArgsConstructor
public class TenantContextServiceImpl implements TenantContextService {

    public static final String HOSPITAL_HEADER = "X-Hospital-Id";

    private final AdminRepo adminRepo;
    private final DoctorRepo doctorRepo;
    private final ReceptionistRepo receptionistRepo;
    private final PatientRepo patientRepo;
    private final HttpServletRequest request;

    @Override
    public UUID getCurrentUserHospitalId() {
        try {
            UserEntity currentUser = SecurityUtils.getCurrentUser();
            if (currentUser == null || currentUser.getId() == null || currentUser.getRole() == null || currentUser.getRole().getRoleName() == null) {
                return null;
            }

            String roleName = currentUser.getRole().getRoleName().trim().toUpperCase(Locale.ROOT);

            if ("ADMIN".equals(roleName)) {
                return resolveAdminActiveHospital(currentUser.getId());
            }

            String selectedHospital = request.getHeader(HOSPITAL_HEADER);
            if (selectedHospital == null || selectedHospital.isBlank()) {
                selectedHospital = request.getParameter("hospitalId");
            }

            UUID requestedHospitalId = null;
            if (selectedHospital != null && !selectedHospital.trim().isBlank()) {
                try {
                    requestedHospitalId = UUID.fromString(selectedHospital.trim());
                } catch (IllegalArgumentException ignored) {
                }
            }

            UUID defaultHospitalId = getHospitalIdForUser(currentUser);

            // Security Hardening: Ensure non-admin users cannot inject arbitrary hospital IDs via header/params.
            // If a hospitalId is specified in headers/params, it must strictly be an authorized hospital for this user.
            if (requestedHospitalId != null) {
                if ("PATIENT".equals(roleName)) {
                    // Patients can access public hospital context or hospitals they have a profile with
                    boolean hasAccess = patientRepo.findByAccountId(currentUser.getId()).stream()
                            .map(p -> p != null ? p.getHospital() : null)
                            .filter(Objects::nonNull)
                            .map(h -> h != null ? h.getId() : null)
                            .anyMatch(requestedHospitalId::equals);
                    return hasAccess ? requestedHospitalId : defaultHospitalId;
                } else if ("DOCTOR".equals(roleName)) {
                    // Doctors can only act in hospitals where they have an active profile/affiliation
                    DoctorEntity doctor = doctorRepo.findByAccountId(currentUser.getId()).orElse(null);
                    boolean hasAccess = doctor != null && doctor.getHospital() != null && requestedHospitalId.equals(doctor.getHospital().getId());
                    return hasAccess ? requestedHospitalId : defaultHospitalId;
                } else if ("RECEPTIONIST".equals(roleName)) {
                    ReceptionistEntity receptionist = receptionistRepo.findByAccountId(currentUser.getId()).orElse(null);
                    boolean hasAccess = receptionist != null && receptionist.getHospital() != null && requestedHospitalId.equals(receptionist.getHospital().getId());
                    return hasAccess ? requestedHospitalId : defaultHospitalId;
                }
            }

            return defaultHospitalId;
        } catch (AccessDeniedException e) {
            throw e;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public UUID getHospitalIdForUser(UserEntity user) {
        if (user == null || user.getId() == null || user.getRole() == null || user.getRole().getRoleName() == null || user.getRole().getRoleName().isBlank()) {
            return null;
        }

        UUID accountId = user.getId();
        String roleName = user.getRole().getRoleName().trim().toUpperCase(Locale.ROOT);

        return switch (roleName) {
            case "ADMIN" -> {
                AdminEntity admin = adminRepo.findFirstByAccountIdOrderByCreatedAtAsc(accountId).orElse(null);
                yield (admin != null && admin.getHospital() != null) ? admin.getHospital().getId() : null;
            }
            case "DOCTOR" -> {
                DoctorEntity doctor = doctorRepo.findByAccountId(accountId).orElse(null);
                yield (doctor != null && doctor.getHospital() != null) ? doctor.getHospital().getId() : null;
            }
            case "RECEPTIONIST" -> {
                ReceptionistEntity receptionist = receptionistRepo.findByAccountId(accountId).orElse(null);
                yield (receptionist != null && receptionist.getHospital() != null) ? receptionist.getHospital().getId() : null;
            }
            case "PATIENT" -> {
                PatientEntity patient = patientRepo.findByAccountId(accountId).orElse(null);
                yield (patient != null && patient.getHospital() != null) ? patient.getHospital().getId() : null;
            }
            default -> null;
        };
    }

    @Override
    public List<UUID> getCurrentUserHospitalIds() {
        try {
            UserEntity currentUser = SecurityUtils.getCurrentUser();
            if (currentUser == null || currentUser.getId() == null || currentUser.getRole() == null || currentUser.getRole().getRoleName() == null) {
                return List.of();
            }

            String roleName = currentUser.getRole().getRoleName().trim().toUpperCase(Locale.ROOT);

            if ("ADMIN".equals(roleName)) {
                return adminRepo.findAllByAccountId(currentUser.getId()).stream()
                        .filter(admin -> admin.getStatus() == AdminEntity.AdminStatus.ACTIVE)
                        .map(admin -> admin != null ? admin.getHospital() : null)
                        .filter(Objects::nonNull)
                        .map(h -> h != null ? h.getId() : null)
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList();
            }

            if ("PATIENT".equals(roleName)) {
                UUID id = getCurrentUserHospitalId();
                return id != null ? List.of(id) : List.of();
            }

            UUID id = getHospitalIdForUser(currentUser);
            return id != null ? List.of(id) : List.of();
        } catch (Exception e) {
            return List.of();
        }
    }

    @Override
    public boolean hasCurrentUserHospitalAccess(UUID hospitalId) {
        return hospitalId != null && getCurrentUserHospitalIds().contains(hospitalId);
    }

    private UUID resolveAdminActiveHospital(UUID accountId) {
        if (accountId == null) {
            return null;
        }

        List<UUID> hospitalIds = adminRepo.findAllByAccountId(accountId).stream()
                .filter(admin -> admin.getStatus() == AdminEntity.AdminStatus.ACTIVE)
                .map(admin -> admin != null ? admin.getHospital() : null)
                .filter(Objects::nonNull)
                .map(h -> h != null ? h.getId() : null)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (hospitalIds.isEmpty()) {
            return null;
        }

        String selectedHospital = request.getHeader(HOSPITAL_HEADER);
        if (selectedHospital == null || selectedHospital.isBlank()) {
            selectedHospital = request.getParameter("hospitalId");
        }

        if (selectedHospital == null || selectedHospital.trim().isBlank()) {
            throw new AccessDeniedException("Multi-hospital admin must explicitly provide a valid X-Hospital-Id header");
        }

        try {
            UUID requestedHospitalId = UUID.fromString(selectedHospital.trim());
            if (hospitalIds.contains(requestedHospitalId)) {
                return requestedHospitalId;
            }
        } catch (IllegalArgumentException ignored) {
        }

        throw new AccessDeniedException("Multi-hospital admin must explicitly provide a valid X-Hospital-Id header");
    }
}
