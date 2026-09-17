package org.proj.service.impl;

import org.proj.dto.ConsentRequest;
import org.proj.dto.ConsentResponse;
import org.proj.entity.AdminAuditLogEntity;
import org.proj.entity.PatientConsentEntity;
import org.proj.entity.PatientConsentEntity.ConsentStatus;
import org.proj.entity.PatientConsentEntity.ConsentType;
import org.proj.entity.PatientEntity;
import org.proj.entity.UserEntity;
import org.proj.repository.PatientConsentRepo;
import org.proj.security.SecurityUtils;
import org.proj.service.AdminAuditLogService;
import org.proj.service.PatientConsentService;
import org.proj.service.PatientService;
import org.proj.service.TenantContextService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PatientConsentServiceImpl implements PatientConsentService {

    private final PatientConsentRepo patientConsentRepo;
    private final PatientService patientService;
    private final TenantContextService tenantContextService;
    private final AdminAuditLogService auditLogService;

    @Override
    @Transactional
    public ConsentResponse updateConsent(ConsentRequest request) {

        UserEntity currentUser = requireCurrentUser();

        UUID hospitalId = requireCurrentHospitalId();

        PatientEntity patient =
                getCurrentPatient(currentUser.getId(), hospitalId);

        PatientConsentEntity consent =
                patientConsentRepo
                        .findByPatientIdAndConsentType(
                                patient.getId(),
                                request.getConsentType()
                        )
                        .orElse(
                                PatientConsentEntity.builder()
                                        .patient(patient)
                                        .consentType(request.getConsentType())
                                        .build()
                        );

        consent.setStatus(request.getStatus());

        if (request.getStatus() == ConsentStatus.GRANTED) {

            consent.setConsentedAt(LocalDateTime.now());
            consent.setRevokedAt(null);

        } else if (request.getStatus() == ConsentStatus.REVOKED) {

            consent.setRevokedAt(LocalDateTime.now());
        }

        PatientConsentEntity saved =
                patientConsentRepo.save(consent);

        logConsentAction(
                saved.getId(),
                "CONSENT_" + request.getStatus().name(),
                currentUser.getId()
        );

        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConsentResponse> getMyConsents() {

        UserEntity currentUser = requireCurrentUser();

        UUID hospitalId = requireCurrentHospitalId();

        PatientEntity patient =
                getCurrentPatient(currentUser.getId(), hospitalId);

        return patientConsentRepo
                .findByPatientIdOrderByCreatedAtDesc(patient.getId())
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ConsentResponse getConsentByType(
            ConsentType type) {

        UserEntity currentUser = requireCurrentUser();

        UUID hospitalId = requireCurrentHospitalId();

        PatientEntity patient =
                getCurrentPatient(currentUser.getId(), hospitalId);

        PatientConsentEntity consent =
                patientConsentRepo
                        .findByPatientIdAndConsentType(
                                patient.getId(),
                                type
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Consent not found for type: " + type
                                )
                        );

        return toResponse(consent);
    }

    @Override
    @Transactional
    public ConsentResponse revokeConsent(
            ConsentType type) {

        UserEntity currentUser = requireCurrentUser();

        UUID hospitalId = requireCurrentHospitalId();

        PatientEntity patient =
                getCurrentPatient(currentUser.getId(), hospitalId);

        PatientConsentEntity consent =
                patientConsentRepo
                        .findByPatientIdAndConsentType(
                                patient.getId(),
                                type
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Consent not found for type: " + type
                                )
                        );

        if (consent.getStatus() != ConsentStatus.REVOKED) {

            consent.setStatus(ConsentStatus.REVOKED);
            consent.setRevokedAt(LocalDateTime.now());

            consent =
                    patientConsentRepo.save(consent);

            logConsentAction(
                    consent.getId(),
                    "CONSENT_REVOKED",
                    currentUser.getId()
            );
        }

        return toResponse(consent);
    }

    @Override
    @Transactional(readOnly = true)
    public void verifyConsent(
            UUID patientId,
            ConsentType type) {

        UUID hospitalId = requireCurrentHospitalId();

        PatientEntity patient =
                patientService.findPatientByIdAndHospitalId(
                        patientId,
                        hospitalId
                );

        PatientConsentEntity consent =
                patientConsentRepo
                        .findByPatientIdAndConsentType(
                                patient.getId(),
                                type
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        type
                                                + " consent is required but has not been provided."
                                )
                        );

        if (consent.getStatus() != ConsentStatus.GRANTED) {

            throw new IllegalArgumentException(
                    type
                            + " consent is required but was "
                            + consent.getStatus()
                            .name()
                            .toLowerCase()
                            + "."
            );
        }
    }

    private UserEntity requireCurrentUser() {

        UserEntity currentUser =
                SecurityUtils.getCurrentUser();

        if (currentUser == null) {

            throw new AccessDeniedException(
                    "Authentication required"
            );
        }

        return currentUser;
    }

    private UUID requireCurrentHospitalId() {

        UUID hospitalId =
                tenantContextService
                        .getCurrentUserHospitalId();

        if (hospitalId == null) {

            throw new AccessDeniedException(
                    "Hospital context is required"
            );
        }

        return hospitalId;
    }

    private PatientEntity getCurrentPatient(
            UUID accountId,
            UUID hospitalId) {

        return patientService
                .findPatientByAccountIdAndHospitalId(
                        accountId,
                        hospitalId
                );
    }

    private void logConsentAction(
            UUID entityId,
            String action,
            UUID performedBy) {

        UUID hospitalId =
                requireCurrentHospitalId();

        PatientEntity patient =
                patientService.findPatientByIdAndHospitalId(
                        getCurrentPatient(performedBy, hospitalId).getId(),
                        hospitalId
                );

        AdminAuditLogEntity log =
                AdminAuditLogEntity.builder()
                        .action(action)
                        .entityName("PatientConsentEntity")
                        .entityId(entityId)
                        .performedBy(performedBy)
                        .hospital(patient.getHospital())
                        .details(
                                "Patient updated consent status"
                        )
                        .build();

        auditLogService.save(log);
    }

    private ConsentResponse toResponse(
            PatientConsentEntity entity) {

        return ConsentResponse.builder()
                .id(entity.getId())
                .consentType(entity.getConsentType())
                .status(entity.getStatus())
                .consentedAt(entity.getConsentedAt())
                .revokedAt(entity.getRevokedAt())
                .build();
    }
}
