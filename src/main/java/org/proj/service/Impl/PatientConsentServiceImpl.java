package org.proj.service.impl;

import org.proj.dto.ConsentRequest;
import org.proj.dto.ConsentRequestDto;
import org.proj.dto.ConsentResponse;
import org.proj.entity.AdminAuditLogEntity;
import org.proj.entity.DoctorEntity;
import org.proj.entity.PatientConsentEntity;
import org.proj.entity.PatientConsentEntity.ConsentStatus;
import org.proj.entity.PatientConsentEntity.ConsentType;
import org.proj.entity.PatientEntity;
import org.proj.entity.UserEntity;
import org.proj.exception.ResourceNotFoundException;
import org.proj.repository.PatientConsentRepo;
import org.proj.security.SecurityUtils;
import org.proj.service.AdminAuditLogService;
import org.proj.service.DoctorService;
import org.proj.service.PatientConsentService;
import org.proj.service.PatientService;
import org.proj.service.TenantContextService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PatientConsentServiceImpl implements PatientConsentService {

    private final PatientConsentRepo patientConsentRepo;
    private final PatientService patientService;
    private final DoctorService doctorService;
    private final TenantContextService tenantContextService;
    private final AdminAuditLogService auditLogService;

    @Override
    @Transactional
    public ConsentResponse grantDoctorConsent(ConsentRequestDto requestDto) {
        try {
            ConsentRequestDto validDto = Optional.ofNullable(requestDto)
                    .orElseThrow(() -> new IllegalArgumentException("Consent request body is required"));

            UUID resolvedDoctorId = null;
            if (validDto.getDoctorId() != null) {
                String docStr = validDto.getDoctorId().toString().trim();
                try {
                    resolvedDoctorId = UUID.fromString(docStr);
                } catch (Exception ignored) {}
            }

            if (resolvedDoctorId == null && validDto.getDoctorId() != null) {
                throw new ResourceNotFoundException("Doctor not found with ID: " + validDto.getDoctorId());
            }

            ConsentType type = ConsentType.MEDICAL_RECORD_SHARING;
            if (validDto.getConsentType() != null && !validDto.getConsentType().isBlank()) {
                String ctStr = validDto.getConsentType().trim().toUpperCase(java.util.Locale.ROOT);
                if (ctStr.equals("MEDICAL_RECORDS") || ctStr.equals("MEDICAL_RECORD") || ctStr.equals("ALL")) {
                    type = ConsentType.MEDICAL_RECORD_SHARING;
                } else {
                    try {
                        type = ConsentType.valueOf(ctStr);
                    } catch (Exception ignored) {
                        type = ConsentType.MEDICAL_RECORD_SHARING;
                    }
                }
            }

            ConsentStatus status = ConsentStatus.GRANTED;
            if (validDto.getStatus() != null && !validDto.getStatus().isBlank()) {
                String stStr = validDto.getStatus().trim().toUpperCase(java.util.Locale.ROOT);
                try {
                    status = ConsentStatus.valueOf(stStr);
                } catch (Exception ignored) {
                    status = ConsentStatus.GRANTED;
                }
            }

            String scope = Optional.ofNullable(validDto.getAccessLevel())
                    .filter(s -> !s.isBlank())
                    .orElseGet(() -> Optional.ofNullable(validDto.getScope()).filter(s -> !s.isBlank()).orElse("ALL_RECORDS"));

            ConsentRequest mapped = ConsentRequest.builder()
                    .consentType(type)
                    .status(status)
                    .doctorId(resolvedDoctorId)
                    .scope(scope)
                    .accessLevel(scope)
                    .version(Optional.ofNullable(validDto.getVersion()).orElse("v1.0"))
                    .build();

            return updateConsent(mapped);
        } catch (AccessDeniedException | IllegalArgumentException | ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            String msg = e.getMessage() != null && !e.getMessage().isBlank()
                    ? e.getMessage()
                    : "Unable to grant doctor consent";
            throw new RuntimeException(msg, e);
        }
    }

    @Override
    @Transactional
    public ConsentResponse toggleStandardConsent(ConsentRequest request) {
        try {
            ConsentRequest validReq = Optional.ofNullable(request)
                    .filter(r -> r.getConsentType() != null)
                    .orElseThrow(() -> new IllegalArgumentException("Consent type is required"));

            UserEntity currentUser = requireCurrentUser();
            UUID hospitalId = Optional.ofNullable(tenantContextService)
                    .map(s -> {
                        try {
                            return s.getCurrentUserHospitalId();
                        } catch (Exception e) {
                            return null;
                        }
                    })
                    .orElse(null);

            PatientEntity patient = getCurrentPatient(currentUser.getId(), hospitalId);

            ConsentType consentType = validReq.getConsentType();
            ConsentStatus status = Optional.ofNullable(validReq.getStatus()).orElse(ConsentStatus.GRANTED);

            PatientConsentEntity consent = patientConsentRepo.findByPatientIdAndConsentTypeAndDoctorIsNullForUpdate(patient.getId(), consentType)
                    .orElseGet(() -> PatientConsentEntity.builder()
                            .patient(patient)
                            .doctor(null)
                            .consentType(consentType)
                            .version(Optional.ofNullable(validReq.getVersion()).orElse("v1.0"))
                            .scope(Optional.ofNullable(validReq.getScope()).orElse("ALL_RECORDS"))
                            .build());

            // Only update if status actually changed or new record
            if (consent.getId() != null && consent.getStatus() == status) {
                return toResponse(consent);
            }

            consent.setStatus(status);
            Optional.ofNullable(validReq.getScope())
                    .filter(s -> !s.isBlank())
                    .ifPresent(consent::setScope);

            Optional.ofNullable(validReq.getVersion())
                    .filter(s -> !s.isBlank())
                    .ifPresent(consent::setVersion);

            LocalDateTime now = LocalDateTime.now();
            Optional.of(status)
                    .filter(ConsentStatus.GRANTED::equals)
                    .ifPresent(s -> {
                        consent.setConsentedAt(now);
                        consent.setRevokedAt(null);
                    });

            Optional.of(status)
                    .filter(ConsentStatus.REVOKED::equals)
                    .ifPresent(s -> consent.setRevokedAt(now));

            PatientConsentEntity saved = patientConsentRepo.save(consent);
            logConsentAction(saved.getId(), "CONSENT_" + status.name(), currentUser.getId());

            return toResponse(saved);
        } catch (AccessDeniedException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            String msg = e.getMessage() != null && !e.getMessage().isBlank()
                    ? e.getMessage()
                    : "Unable to toggle standard consent";
            throw new RuntimeException(msg, e);
        }
    }

    @Override
    @Transactional
    public ConsentResponse revokeDoctorConsent(Object doctorId) {
        try {
            Object validDoctorId = Optional.ofNullable(doctorId)
                    .orElseThrow(() -> new IllegalArgumentException("Doctor ID is required"));

            UserEntity currentUser = requireCurrentUser();
            UUID hospitalId = Optional.ofNullable(tenantContextService)
                    .map(s -> {
                        try {
                            return s.getCurrentUserHospitalId();
                        } catch (Exception e) {
                            return null;
                        }
                    })
                    .orElse(null);

            PatientEntity patient = getCurrentPatient(currentUser.getId(), hospitalId);

            final UUID targetDocUuid;
            try {
                targetDocUuid = UUID.fromString(validDoctorId.toString().trim());
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid doctor identifier: " + validDoctorId);
            }

            DoctorEntity doctor = doctorService.findDoctorByIdOrAccountId(targetDocUuid).orElseGet(() -> {
                try {
                    return doctorService.findDoctorById(targetDocUuid);
                } catch (Exception e) {
                    return null;
                }
            });

            if (doctor == null) {
                throw new ResourceNotFoundException("Doctor not found with ID: " + validDoctorId);
            }

            UUID actualDoctorId = doctor.getId();

            List<PatientConsentEntity> records = new ArrayList<>(patientConsentRepo.findByPatientIdAndDoctorId(patient.getId(), actualDoctorId));

            LocalDateTime now = LocalDateTime.now();
            PatientConsentEntity resultEntity;

            if (records.isEmpty()) {
                PatientConsentEntity newRevoked = PatientConsentEntity.builder()
                        .patient(patient)
                        .doctor(doctor)
                        .consentType(ConsentType.MEDICAL_RECORD_SHARING)
                        .status(ConsentStatus.REVOKED)
                        .consentedAt(now)
                        .revokedAt(now)
                        .scope("ALL_RECORDS")
                        .version("v1.0")
                        .build();
                resultEntity = patientConsentRepo.save(newRevoked);
                logConsentAction(resultEntity.getId(), "CONSENT_REVOKED", currentUser.getId());
            } else {
                boolean anyChanged = false;
                for (PatientConsentEntity record : records) {
                    if (record.getStatus() != ConsentStatus.REVOKED) {
                        record.setStatus(ConsentStatus.REVOKED);
                        record.setRevokedAt(now);
                        record.setUpdatedAt(now);
                        anyChanged = true;
                    }
                }
                if (anyChanged) {
                    patientConsentRepo.saveAll(records);
                    for (PatientConsentEntity rec : records) {
                        logConsentAction(rec.getId(), "CONSENT_REVOKED", currentUser.getId());
                    }
                }
                resultEntity = records.get(0);
            }

            return toResponse(resultEntity);
        } catch (AccessDeniedException | IllegalArgumentException | ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            String msg = e.getMessage() != null && !e.getMessage().isBlank()
                    ? e.getMessage()
                    : "Unable to revoke doctor consent";
            throw new RuntimeException(msg, e);
        }
    }

    @Override
    @Transactional
    public ConsentResponse updateConsent(ConsentRequest request) {
        try {
            ConsentRequest validReq = Optional.ofNullable(request)
                    .orElseThrow(() -> new IllegalArgumentException("Consent request is required"));

            UserEntity currentUser = requireCurrentUser();
            UUID hospitalId = Optional.ofNullable(tenantContextService)
                    .map(s -> {
                        try {
                            return s.getCurrentUserHospitalId();
                        } catch (Exception e) {
                            return null;
                        }
                    })
                    .orElse(null);

            PatientEntity patient = getCurrentPatient(currentUser.getId(), hospitalId);

            DoctorEntity doctor = Optional.ofNullable(validReq.getDoctorId())
                    .map(docId -> doctorService.findDoctorByIdOrAccountId(docId).orElseGet(() -> {
                        try {
                            return doctorService.findDoctorById(docId);
                        } catch (Exception e) {
                            throw new ResourceNotFoundException("Doctor not found with ID: " + docId);
                        }
                    }))
                    .orElse(null);

            final DoctorEntity finalDoctor = doctor;
            PatientConsentEntity consent = Optional.ofNullable(doctor)
                    .map(doc -> patientConsentRepo
                            .findFirstByPatientIdAndConsentTypeAndDoctorId(patient.getId(), validReq.getConsentType(), doc.getId())
                            .orElseGet(() -> PatientConsentEntity.builder()
                                    .patient(patient)
                                    .doctor(finalDoctor)
                                    .consentType(validReq.getConsentType())
                                    .build()))
                    .orElseGet(() -> patientConsentRepo
                            .findByPatientIdAndConsentTypeAndDoctorIsNull(patient.getId(), validReq.getConsentType())
                            .orElseGet(() -> PatientConsentEntity.builder()
                                    .patient(patient)
                                    .consentType(validReq.getConsentType())
                                    .build()));

            consent.setStatus(validReq.getStatus());
            if (validReq.getScope() != null && !validReq.getScope().isBlank()) {
                consent.setScope(validReq.getScope().trim());
            }

            if (validReq.getVersion() != null && !validReq.getVersion().isBlank()) {
                consent.setVersion(validReq.getVersion().trim());
            }

            if (ConsentStatus.GRANTED.equals(validReq.getStatus())) {
                consent.setConsentedAt(LocalDateTime.now());
                consent.setRevokedAt(null);
            } else if (ConsentStatus.REVOKED.equals(validReq.getStatus())) {
                consent.setRevokedAt(LocalDateTime.now());
            }

            PatientConsentEntity saved = patientConsentRepo.save(consent);
            String statusStr = (validReq.getStatus() != null) ? validReq.getStatus().name() : "UNKNOWN";
            logConsentAction(saved.getId(), "CONSENT_" + statusStr, currentUser.getId());

            return toResponse(saved);
        } catch (AccessDeniedException | IllegalArgumentException | ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            String msg = e.getMessage() != null && !e.getMessage().isBlank()
                    ? e.getMessage()
                    : "Unable to update consent";
            throw new RuntimeException(msg, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConsentResponse> getMyConsents() {
        try {
            UserEntity currentUser = requireCurrentUser();
            UUID hospitalId = Optional.ofNullable(tenantContextService)
                    .map(s -> {
                        try {
                            return s.getCurrentUserHospitalId();
                        } catch (Exception e) {
                            return null;
                        }
                    })
                    .orElse(null);

            PatientEntity patient = getCurrentPatient(currentUser.getId(), hospitalId);

            return patientConsentRepo.findByPatientIdOrderByCreatedAtDesc(patient.getId()).stream()
                    .map(this::toResponse)
                    .collect(Collectors.toList());
        } catch (AccessDeniedException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            String msg = e.getMessage() != null && !e.getMessage().isBlank()
                    ? e.getMessage()
                    : "Unable to retrieve consents";
            throw new RuntimeException(msg, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ConsentResponse getConsentByType(ConsentType type) {
        try {
            ConsentType validType = Optional.ofNullable(type)
                    .orElseThrow(() -> new IllegalArgumentException("Consent type is required"));

            UserEntity currentUser = requireCurrentUser();
            UUID hospitalId = Optional.ofNullable(tenantContextService)
                    .map(s -> {
                        try {
                            return s.getCurrentUserHospitalId();
                        } catch (Exception e) {
                            return null;
                        }
                    })
                    .orElse(null);

            PatientEntity patient = getCurrentPatient(currentUser.getId(), hospitalId);

            PatientConsentEntity consent = patientConsentRepo
                    .findByPatientIdAndConsentTypeAndDoctorIsNull(patient.getId(), validType)
                    .orElseGet(() -> patientConsentRepo.findAllByPatientIdAndConsentType(patient.getId(), validType).stream()
                            .filter(c -> c.getDoctor() == null)
                            .findFirst()
                            .orElseThrow(() -> new IllegalArgumentException("Consent not found for type: " + validType)));

            return toResponse(consent);
        } catch (AccessDeniedException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            String msg = e.getMessage() != null && !e.getMessage().isBlank()
                    ? e.getMessage()
                    : "Unable to retrieve consent by type";
            throw new RuntimeException(msg, e);
        }
    }

    @Override
    @Transactional
    public ConsentResponse revokeConsent(ConsentType type) {
        try {
            ConsentType validType = Optional.ofNullable(type)
                    .orElseThrow(() -> new IllegalArgumentException("Consent type is required"));

            UserEntity currentUser = requireCurrentUser();
            UUID hospitalId = Optional.ofNullable(tenantContextService)
                    .map(s -> {
                        try {
                            return s.getCurrentUserHospitalId();
                        } catch (Exception e) {
                            return null;
                        }
                    })
                    .orElse(null);

            PatientEntity patient = getCurrentPatient(currentUser.getId(), hospitalId);

            PatientConsentEntity consent = patientConsentRepo
                    .findByPatientIdAndConsentTypeAndDoctorIsNull(patient.getId(), validType)
                    .orElseGet(() -> patientConsentRepo.findAllByPatientIdAndConsentType(patient.getId(), validType).stream()
                            .filter(c -> c.getDoctor() == null)
                            .findFirst()
                            .orElseGet(() -> PatientConsentEntity.builder()
                                    .patient(patient)
                                    .doctor(null)
                                    .consentType(validType)
                                    .scope("ALL_RECORDS")
                                    .version("v1.0")
                                    .consentedAt(LocalDateTime.now())
                                    .status(ConsentStatus.REVOKED)
                                    .build()));

            consent.setStatus(ConsentStatus.REVOKED);
            consent.setRevokedAt(LocalDateTime.now());
            PatientConsentEntity saved = patientConsentRepo.save(consent);

            logConsentAction(saved.getId(), "CONSENT_REVOKED", currentUser.getId());
            return toResponse(saved);
        } catch (AccessDeniedException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            String msg = e.getMessage() != null && !e.getMessage().isBlank()
                    ? e.getMessage()
                    : "Unable to revoke consent";
            throw new RuntimeException(msg, e);
        }
    }

    @Override
    @Transactional
    public ConsentResponse revokeConsentForDoctor(ConsentType type, UUID doctorId) {
        try {
            ConsentType validType = Optional.ofNullable(type)
                    .orElseThrow(() -> new IllegalArgumentException("Consent type is required"));
            UUID validDoctorId = Optional.ofNullable(doctorId)
                    .orElseThrow(() -> new IllegalArgumentException("Doctor ID is required"));

            UserEntity currentUser = requireCurrentUser();
            UUID hospitalId = Optional.ofNullable(tenantContextService)
                    .map(s -> {
                        try {
                            return s.getCurrentUserHospitalId();
                        } catch (Exception e) {
                            return null;
                        }
                    })
                    .orElse(null);

            PatientEntity patient = getCurrentPatient(currentUser.getId(), hospitalId);

            PatientConsentEntity consent = patientConsentRepo
                    .findFirstByPatientIdAndConsentTypeAndDoctorId(patient.getId(), validType, validDoctorId)
                    .orElseThrow(() -> new IllegalArgumentException("Consent not found for type: " + validType + " and doctor: " + validDoctorId));

            if (consent.getStatus() != ConsentStatus.REVOKED) {
                consent.setStatus(ConsentStatus.REVOKED);
                consent.setRevokedAt(LocalDateTime.now());
                consent = patientConsentRepo.save(consent);
                logConsentAction(consent.getId(), "CONSENT_REVOKED", currentUser.getId());
            }

            return toResponse(consent);
        } catch (AccessDeniedException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            String msg = e.getMessage() != null && !e.getMessage().isBlank()
                    ? e.getMessage()
                    : "Unable to revoke doctor consent";
            throw new RuntimeException(msg, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void verifyConsent(UUID patientId, ConsentType type) {
        PatientConsentEntity consent = patientConsentRepo
                .findByPatientIdAndConsentTypeAndDoctorIsNull(patientId, type)
                .orElseGet(() -> patientConsentRepo.findAllByPatientIdAndConsentType(patientId, type).stream()
                        .filter(c -> c.getDoctor() == null)
                        .findFirst()
                        .orElseThrow(() -> new AccessDeniedException(type + " consent is required but has not been provided.")));

        Optional.of(consent.getStatus())
                .filter(ConsentStatus.GRANTED::equals)
                .orElseThrow(() -> new AccessDeniedException(type + " consent is required but was " + consent.getStatus().name().toLowerCase() + "."));
    }

    @Override
    @Transactional(readOnly = true)
    public void verifyConsentForDoctor(UUID patientId, UUID doctorId, ConsentType type, String requiredScope) {
        // 1. Check doctor-specific consent first
        if (doctorId != null) {
            Optional<PatientConsentEntity> docConsentOpt = patientConsentRepo.findFirstByPatientIdAndConsentTypeAndDoctorId(patientId, type, doctorId);

            if (docConsentOpt.isPresent()) {
                PatientConsentEntity docConsent = docConsentOpt.get();
                Optional.of(docConsent.getStatus())
                        .filter(status -> status != ConsentStatus.REVOKED)
                        .orElseThrow(() -> new AccessDeniedException("Patient has revoked " + type + " consent for this doctor."));

                if (docConsent.getStatus() == ConsentStatus.GRANTED) {
                    if (requiredScope != null && docConsent.getScope() != null
                            && !docConsent.getScope().equalsIgnoreCase("ALL")
                            && !docConsent.getScope().toUpperCase().contains(requiredScope.toUpperCase())) {
                        throw new AccessDeniedException("Consent scope '" + docConsent.getScope() + "' does not cover '" + requiredScope + "'.");
                    }
                    return;
                }
            }
        }

        // 2. Check general patient consent for this type
        PatientConsentEntity generalConsent = patientConsentRepo
                .findByPatientIdAndConsentTypeAndDoctorIsNull(patientId, type)
                .orElseGet(() -> patientConsentRepo.findAllByPatientIdAndConsentType(patientId, type).stream()
                        .filter(c -> c.getDoctor() == null)
                        .findFirst()
                        .orElseThrow(() -> new AccessDeniedException(type + " consent has not been granted by patient.")));

        Optional.of(generalConsent.getStatus())
                .filter(ConsentStatus.GRANTED::equals)
                .orElseThrow(() -> new AccessDeniedException(type + " consent is " + generalConsent.getStatus().name().toLowerCase() + "."));

        if (requiredScope != null && generalConsent.getScope() != null
                && !generalConsent.getScope().equalsIgnoreCase("ALL")
                && !generalConsent.getScope().toUpperCase().contains(requiredScope.toUpperCase())) {
            throw new AccessDeniedException("General consent scope '" + generalConsent.getScope() + "' does not cover '" + requiredScope + "'.");
        }
    }

    private UserEntity requireCurrentUser() {
        return Optional.ofNullable(SecurityUtils.getCurrentUser())
                .orElseThrow(() -> new AccessDeniedException("Authentication required"));
    }

    private PatientEntity getCurrentPatient(UUID accountId, UUID hospitalId) {
        if (hospitalId != null) {
            try {
                return patientService.findPatientByAccountIdAndHospitalId(accountId, hospitalId);
            } catch (Exception ignored) {}
        }
        return patientService.findPatientByAccountId(accountId);
    }

    private void logConsentAction(UUID entityId, String action, UUID performedBy) {
        try {
            UUID hospitalId = null;
            try {
                hospitalId = tenantContextService.getCurrentUserHospitalId();
            } catch (Exception ignored) {}

            PatientEntity patient = getCurrentPatient(performedBy, hospitalId);

            AdminAuditLogEntity log = AdminAuditLogEntity.builder()
                    .action(action)
                    .entityName("PatientConsentEntity")
                    .entityId(entityId)
                    .performedBy(performedBy)
                    .hospital((patient != null) ? patient.getHospital() : null)
                    .details("Patient updated consent status")
                    .build();

            auditLogService.save(log);
        } catch (Exception ignored) {}
    }

    private ConsentResponse toResponse(PatientConsentEntity entity) {
        String docName = null;
        UUID docId = null;
        Map<String, Object> docMap = null;

        if (entity.getDoctor() != null) {
            docId = entity.getDoctor().getId();
            UserEntity acc = entity.getDoctor().getAccount();
            String fName = "";
            String lName = "";
            if (acc != null) {
                fName = Optional.ofNullable(acc.getFirstName()).orElse("");
                lName = Optional.ofNullable(acc.getLastName()).orElse("");
                docName = (fName + " " + lName).trim();
                if (docName.isBlank()) {
                    docName = acc.getEmail();
                }
            }
            docMap = Map.of(
                    "id", docId.toString(),
                    "user", Map.of(
                            "fullName", (docName != null && !docName.isBlank()) ? docName : "Doctor",
                            "firstName", fName,
                            "lastName", lName
                    )
            );
        }

        return ConsentResponse.builder()
                .id(entity.getId())
                .consentType(entity.getConsentType())
                .status(entity.getStatus())
                .doctorId(docId)
                .doctorName(docName)
                .scope(entity.getScope())
                .accessLevel(entity.getScope())
                .version(entity.getVersion())
                .consentedAt(entity.getConsentedAt())
                .revokedAt(entity.getRevokedAt())
                .doctor(docMap)
                .build();
    }
}
