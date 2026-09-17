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

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TenantContextServiceImpl
        implements TenantContextService {

    public static final String HOSPITAL_HEADER =
            "X-Hospital-Id";

    private final AdminRepo adminRepo;
    private final DoctorRepo doctorRepo;
    private final ReceptionistRepo receptionistRepo;
    private final PatientRepo patientRepo;

    private final HttpServletRequest request;

    @Override
    public UUID getCurrentUserHospitalId() {

        UserEntity currentUser =
                SecurityUtils.getCurrentUser();

        if (currentUser == null
                || currentUser.getId() == null
                || currentUser.getRole() == null
                || currentUser.getRole().getRoleName() == null) {

            return null;
        }

        String roleName =
                currentUser.getRole()
                        .getRoleName()
                        .trim()
                        .toUpperCase(Locale.ROOT);

        if ("ADMIN".equals(roleName)) {

            return resolveAdminActiveHospital(
                    currentUser.getId()
            );
        }

        return getHospitalIdForUser(currentUser);
    }

    @Override
    public UUID getHospitalIdForUser(
            UserEntity user) {

        if (user == null
                || user.getId() == null
                || user.getRole() == null
                || user.getRole().getRoleName() == null
                || user.getRole()
                        .getRoleName()
                        .isBlank()) {

            return null;
        }

        UUID accountId =
                user.getId();

        String roleName =
                user.getRole()
                        .getRoleName()
                        .trim()
                        .toUpperCase(Locale.ROOT);

        return switch (roleName) {

            case "ADMIN" ->
                    adminRepo
                            .findFirstByAccountIdOrderByCreatedAtAsc(
                                    accountId
                            )
                            .map(admin ->
                                    admin.getHospital()
                            )
                            .map(hospital ->
                                    hospital.getId()
                            )
                            .orElse(null);

            case "DOCTOR" ->
                    doctorRepo
                            .findByAccountId(accountId)
                            .map(doctor ->
                                    doctor.getHospital()
                            )
                            .map(hospital ->
                                    hospital.getId()
                            )
                            .orElse(null);

            case "RECEPTIONIST" ->
                    receptionistRepo
                            .findByAccountId(accountId)
                            .map(receptionist ->
                                    receptionist.getHospital()
                            )
                            .map(hospital ->
                                    hospital.getId()
                            )
                            .orElse(null);

            case "PATIENT" ->
                    patientRepo
                            .findByAccountId(accountId)
                            .map(patient ->
                                    patient.getHospital()
                            )
                            .map(hospital ->
                                    hospital.getId()
                            )
                            .orElse(null);

            default ->
                    null;
        };
    }

    @Override
    public List<UUID> getCurrentUserHospitalIds() {

        UserEntity currentUser =
                SecurityUtils.getCurrentUser();

        if (currentUser == null
                || currentUser.getId() == null
                || currentUser.getRole() == null
                || currentUser.getRole().getRoleName() == null) {

            return List.of();
        }

        String roleName =
                currentUser.getRole()
                        .getRoleName()
                        .trim()
                        .toUpperCase(Locale.ROOT);

        if ("ADMIN".equals(roleName)) {

            return adminRepo
                    .findAllByAccountId(currentUser.getId())
                    .stream()
                    .filter(admin ->
                            admin.getStatus()
                                    == org.proj.entity.AdminEntity.AdminStatus.ACTIVE)
                    .map(admin ->
                            admin.getHospital())
                    .filter(Objects::nonNull)
                    .map(hospital ->
                            hospital.getId())
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();
        }

        UUID hospitalId =
                getHospitalIdForUser(currentUser);

        return hospitalId == null
                ? List.of()
                : List.of(hospitalId);
    }

    @Override
    public boolean hasCurrentUserHospitalAccess(
            UUID hospitalId) {

        if (hospitalId == null) {
            return false;
        }

        return getCurrentUserHospitalIds()
                .contains(hospitalId);
    }

    private UUID resolveAdminActiveHospital(
            UUID accountId) {

        if (accountId == null) {
            return null;
        }

        List<UUID> hospitalIds =
                adminRepo
                        .findAllByAccountId(accountId)
                        .stream()
                        .filter(admin ->
                                admin.getStatus()
                                        == org.proj.entity.AdminEntity.AdminStatus.ACTIVE)
                        .map(admin ->
                                admin.getHospital()
                        )
                        .filter(Objects::nonNull)
                        .map(hospital ->
                                hospital.getId()
                        )
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList();

        if (hospitalIds.isEmpty()) {
            return null;
        }

        String selectedHospital =
                request.getHeader(HOSPITAL_HEADER);

        if (selectedHospital != null
                && !selectedHospital.isBlank()) {

            try {

                UUID requestedHospitalId =
                        UUID.fromString(
                                selectedHospital.trim()
                        );

                if (hospitalIds.contains(
                        requestedHospitalId)) {

                    return requestedHospitalId;
                }

                return null;

            } catch (IllegalArgumentException e) {

                return null;
            }
        }

        if (hospitalIds.size() == 1) {
            return hospitalIds.get(0);
        }

        return null;
    }
}
