package org.proj.service.impl;

import lombok.RequiredArgsConstructor;
import org.proj.dto.PatientRequest;
import org.proj.dto.PatientResponse;
import org.proj.entity.HospitalEntity;
import org.proj.entity.PatientEntity;
import org.proj.entity.UserEntity;
import org.proj.mapper.PatientMapper;
import org.proj.repository.PatientRepo;
import org.proj.security.SecurityUtils;
import org.proj.service.HospitalService;
import org.proj.service.PatientService;
import org.proj.service.TenantContextService;
import org.proj.service.UserService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PatientServiceImpl implements PatientService {

        private final PatientRepo patientRepo;
        private final UserService userService;
        private final HospitalService hospitalService;
        private final PatientMapper patientMapper;
        private final TenantContextService tenantContextService;

        @Override
        @Transactional
        public PatientResponse createPatient(PatientRequest request) {
                try {
                        PatientRequest validRequest = Optional.ofNullable(request)
                                        .orElseThrow(() -> new IllegalArgumentException("Patient request is required"));

                        UserEntity currentUser = requireCurrentUser();
                        String role = getRole(currentUser);

                        Optional.of(role)
                                        .filter("PATIENT"::equals)
                                        .ifPresent(r -> validRequest.setAccountId(currentUser.getId()));

                        UUID accountId = Optional.ofNullable(validRequest.getAccountId())
                                        .orElseThrow(() -> new IllegalArgumentException("Account ID is required"));

                        UserEntity account = Optional.ofNullable(userService.findUserById(accountId))
                                        .filter(acc -> acc.getRole() != null && "PATIENT".equalsIgnoreCase(acc.getRole().getRoleName()))
                                        .orElseThrow(() -> new IllegalArgumentException("Account is not assigned PATIENT role."));

                        if (patientRepo.existsByAccountId(accountId)) {
                                throw new IllegalArgumentException("Patient profile already exists for this account");
                        }

                        boolean isStaff = "ADMIN".equals(role) || "RECEPTIONIST".equals(role);
                        boolean isPatient = "PATIENT".equals(role);

                        if (!isStaff && !isPatient) {
                                throw new AccessDeniedException("You are not authorized to create a patient profile");
                        }

                        UUID targetHospitalId;
                        if (isStaff) {
                                targetHospitalId = requireCurrentHospital();
                                validRequest.setHospitalId(targetHospitalId);
                        } else {
                                targetHospitalId = Optional.ofNullable(validRequest.getHospitalId())
                                                .orElseThrow(() -> new IllegalArgumentException("Hospital ID is required"));
                        }

                        HospitalEntity hospital = Optional.ofNullable(isPatient
                                        ? hospitalService.findPublicHospitalById(targetHospitalId)
                                        : hospitalService.findHospitalById(targetHospitalId))
                                        .orElseThrow(() -> new IllegalArgumentException("Hospital not found"));

                        String pCode = validRequest.getPatientCode() != null ? validRequest.getPatientCode().trim() : "";
                        if (!pCode.isEmpty()) {
                            if (patientRepo.existsByPatientCodeAndHospitalId(pCode, targetHospitalId)) {
                                throw new IllegalArgumentException("Patient code already exists in this hospital");
                            }
                        } else {
                            validRequest.setPatientCode("PAT" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase());
                        }

                        PatientEntity patient = patientMapper.toEntity(validRequest, account, hospital);
                        PatientEntity savedPatient = patientRepo.save(patient);

                        PatientResponse response = patientMapper.toResponse(savedPatient);
                        response.setMessage("Patient profile created successfully");
                        return response;

                } catch (AccessDeniedException | IllegalArgumentException e) {
                        throw e;
                } catch (Exception e) {
                        String msg = e.getMessage() != null && !e.getMessage().isBlank()
                                        ? e.getMessage()
                                        : "Unable to create patient profile.";
                        throw new RuntimeException(msg, e);
                }
        }

        @Override
        @Transactional(readOnly = true)
        public PatientResponse getPatientById(UUID id) {
                try {
                        UUID validId = Optional.ofNullable(id)
                                        .orElseThrow(() -> new IllegalArgumentException("Patient Id is required"));

                        UserEntity currentUser = requireCurrentUser();
                        String role = getRole(currentUser);
                        UUID currentHospitalId = requireCurrentHospital();

                        PatientEntity patient = patientRepo.findByIdAndHospitalId(validId, currentHospitalId)
                                        .orElseThrow(() -> new AccessDeniedException(
                                                        "You are not authorized to view a patient from another hospital"));

                        if ("PATIENT".equals(role)) {
                                UserEntity patientAcc = (patient != null) ? patient.getAccount() : null;
                                UUID patientUserId = (patientAcc != null) ? patientAcc.getId() : null;
                                if (patientUserId == null || currentUser == null || !patientUserId.equals(currentUser.getId())) {
                                        throw new AccessDeniedException("Patients can access only their own profile");
                                }
                        }

                        return patientMapper.toResponse(patient);

                } catch (AccessDeniedException | IllegalArgumentException e) {
                        throw e;
                } catch (Exception e) {
                        String msg = e.getMessage() != null && !e.getMessage().isBlank()
                                        ? e.getMessage()
                                        : "Unable to fetch patient profile.";
                        throw new RuntimeException(msg, e);
                }
        }

        @Override
        @Transactional(readOnly = true)
        public List<PatientResponse> getAllPatients() {
                try {
                        UserEntity currentUser = requireCurrentUser();
                        String role = getRole(currentUser);

                        return Optional.of(role)
                                        .filter("PATIENT"::equals)
                                        .map(r -> patientRepo.findByAccountId(currentUser.getId()).stream()
                                                        .map(patientMapper::toResponse)
                                                        .toList())
                                        .orElseGet(() -> {
                                                UUID hospitalId = requireCurrentHospital();
                                                return patientRepo.findByHospitalId(hospitalId).stream()
                                                                .map(patientMapper::toResponse)
                                                                .toList();
                                        });

                } catch (AccessDeniedException | IllegalArgumentException e) {
                        throw e;
                } catch (Exception e) {
                        String msg = e.getMessage() != null && !e.getMessage().isBlank()
                                        ? e.getMessage()
                                        : "Unable to fetch patient list.";
                        throw new RuntimeException(msg, e);
                }
        }

        @Override
        @Transactional
        public PatientResponse updatePatient(UUID id, PatientRequest request) {
                try {
                        UUID validId = Optional.ofNullable(id)
                                        .orElseThrow(() -> new IllegalArgumentException("Patient Id is required"));
                        PatientRequest validRequest = Optional.ofNullable(request)
                                        .orElseThrow(() -> new IllegalArgumentException("Patient request is required"));

                        UserEntity currentUser = requireCurrentUser();
                        String role = getRole(currentUser);
                        UUID currentHospitalId = requireCurrentHospital();

                        PatientEntity patient = patientRepo.findByIdAndHospitalId(validId, currentHospitalId)
                                        .orElseThrow(() -> new AccessDeniedException(
                                                        "You are not authorized to update a patient from another hospital"));

                        Optional.of(role)
                                        .filter(r -> "PATIENT".equals(r) || "ADMIN".equals(r) || "RECEPTIONIST".equals(r))
                                        .orElseThrow(() -> new AccessDeniedException(
                                                        "You are not authorized to update this patient profile"));

                        if ("PATIENT".equals(role)) {
                                UserEntity patientAcc = (patient != null) ? patient.getAccount() : null;
                                UUID patientUserId = (patientAcc != null) ? patientAcc.getId() : null;
                                if (patientUserId == null || currentUser == null || !patientUserId.equals(currentUser.getId())) {
                                        throw new AccessDeniedException("You are not authorized to update this patient profile");
                                }
                        }

                        UserEntity initialAccount = (patient != null && patient.getAccount() != null)
                                        ? patient.getAccount()
                                        : null;
                        if (initialAccount == null) {
                                throw new IllegalArgumentException("Patient account is not configured");
                        }

                        UserEntity account = Optional.ofNullable(validRequest.getAccountId())
                                        .filter(newAccId -> !newAccId.equals(initialAccount.getId()))
                                        .map(newAccId -> {
                                                Optional.of(role)
                                                                .filter(r -> !"PATIENT".equals(r))
                                                                .orElseThrow(() -> new AccessDeniedException(
                                                                                "Patients cannot change account ownership"));

                                                UserEntity replacementAccount = Optional.ofNullable(userService.findUserById(newAccId))
                                                                .filter(acc -> acc.getRole() != null && "PATIENT".equalsIgnoreCase(acc.getRole().getRoleName()))
                                                                .orElseThrow(() -> new IllegalArgumentException("Account is not assigned PATIENT role."));

                                                Optional.of(patientRepo.existsByAccountIdAndIdNot(newAccId, validId))
                                                                .filter(exists -> !exists)
                                                                .orElseThrow(() -> new IllegalArgumentException("Patient profile already exists for this account"));

                                                return replacementAccount;
                                        })
                                        .orElse(initialAccount);

                        HospitalEntity hospital = (patient != null) ? patient.getHospital() : null;
                        if (hospital == null || !currentHospitalId.equals(hospital.getId())) {
                                throw new AccessDeniedException(
                                                "Patient does not belong to the current hospital");
                        }

                        Optional.ofNullable(validRequest.getHospitalId())
                                        .filter(reqHospitalId -> !currentHospitalId.equals(reqHospitalId))
                                        .ifPresent(reqHospitalId -> {
                                                throw new AccessDeniedException("Patient hospital cannot be changed");
                                        });

                        String currentPatientCode = (patient != null) ? patient.getPatientCode() : null;
                        var currentPatientStatus = (patient != null) ? patient.getStatus() : null;

                        if ("PATIENT".equals(role)) {
                                if (validRequest.getPatientCode() != null && !validRequest.getPatientCode().equals(currentPatientCode)) {
                                        throw new AccessDeniedException("Patients cannot change patient code");
                                }

                                if (validRequest.getStatus() != null && !validRequest.getStatus().equals(currentPatientStatus)) {
                                        throw new AccessDeniedException("Patients cannot change patient status");
                                }
                        }

                        String targetCode = validRequest.getPatientCode() != null
                                        ? validRequest.getPatientCode()
                                        : currentPatientCode;

                        if (patientRepo.existsByPatientCodeAndHospitalIdAndIdNot(targetCode, currentHospitalId, validId)) {
                                throw new IllegalArgumentException("Patient code already exists in this hospital");
                        }

                        patientMapper.updateEntity(patient, validRequest, account, hospital);

                        if (account != null) {
                                boolean updated = false;
                                if (validRequest.getProfilePicture() != null) {
                                        account.setProfilePicture(validRequest.getProfilePicture());
                                        updated = true;
                                }

                                if (validRequest.getPhoneNumber() != null) {
                                        String phone = validRequest.getPhoneNumber().trim();
                                        if (!phone.isEmpty()) {
                                                account.setPhoneNumber(phone);
                                                updated = true;
                                        }
                                }

                                if (updated) {
                                        userService.save(account);
                                }
                        }

                        PatientEntity updatedPatient = patientRepo.save(patient);
                        PatientResponse response = patientMapper.toResponse(updatedPatient);
                        response.setMessage("Patient profile updated successfully");

                        return response;

                } catch (AccessDeniedException | IllegalArgumentException e) {
                        throw e;
                } catch (Exception e) {
                        String msg = e.getMessage() != null && !e.getMessage().isBlank()
                                        ? e.getMessage()
                                        : "Unable to update patient profile.";
                        throw new RuntimeException(msg, e);
                }
        }

        @Override
        @Transactional
        public void deletePatient(UUID id) {
                try {
                        UUID validId = Optional.ofNullable(id)
                                        .orElseThrow(() -> new IllegalArgumentException("Patient Id is required"));

                        UUID currentHospitalId = requireCurrentHospital();

                        PatientEntity patient = patientRepo.findByIdAndHospitalId(validId, currentHospitalId)
                                        .orElseThrow(() -> new AccessDeniedException(
                                                        "You are not authorized to delete a patient from another hospital"));

                        patient.setStatus(PatientEntity.PatientStatus.INACTIVE);
                        patientRepo.save(patient);

                } catch (AccessDeniedException | IllegalArgumentException e) {
                        throw e;
                } catch (Exception e) {
                        String msg = e.getMessage() != null && !e.getMessage().isBlank()
                                        ? e.getMessage()
                                        : "Unable to delete patient profile.";
                        throw new RuntimeException(msg, e);
                }
        }

        @Override
        @Transactional(readOnly = true)
        public PatientEntity findPatientById(UUID patientId) {
                try {
                        return Optional.ofNullable(patientId)
                                        .flatMap(patientRepo::findById)
                                        .orElseThrow(() -> new IllegalArgumentException(
                                                        patientId == null ? "Patient Id is required" : "Patient not found"));
                } catch (IllegalArgumentException e) {
                        throw e;
                } catch (Exception e) {
                        String msg = e.getMessage() != null && !e.getMessage().isBlank()
                                        ? e.getMessage()
                                        : "Unable to find patient";
                        throw new RuntimeException(msg, e);
                }
        }

        @Override
        @Transactional(readOnly = true)
        public PatientEntity findPatientByIdAndHospitalId(UUID patientId, UUID hospitalId) {
                try {
                        UUID validPatientId = Optional.ofNullable(patientId)
                                        .orElseThrow(() -> new IllegalArgumentException("Patient Id is required"));
                        UUID validHospitalId = Optional.ofNullable(hospitalId)
                                        .orElseThrow(() -> new AccessDeniedException("Hospital context is required"));

                        return patientRepo.findByIdAndHospitalId(validPatientId, validHospitalId)
                                        .orElseThrow(() -> new AccessDeniedException(
                                                        "Patient does not belong to the current hospital"));
                } catch (AccessDeniedException | IllegalArgumentException e) {
                        throw e;
                } catch (Exception e) {
                        String msg = e.getMessage() != null && !e.getMessage().isBlank()
                                        ? e.getMessage()
                                        : "Unable to find patient in hospital";
                        throw new RuntimeException(msg, e);
                }
        }

        @Override
        @Transactional(readOnly = true)
        public PatientResponse getPatientByAccountId(UUID accountId) {
                try {
                        UUID validAccountId = Optional.ofNullable(accountId)
                                        .orElseThrow(() -> new IllegalArgumentException("Account Id is required"));

                        UserEntity currentUser = requireCurrentUser();
                        String role = getRole(currentUser);
                        UUID currentHospitalId = requireCurrentHospital();

                        Optional.of(role)
                                        .filter("PATIENT"::equals)
                                        .filter(r -> !currentUser.getId().equals(validAccountId))
                                        .ifPresent(r -> {
                                                throw new AccessDeniedException(
                                                                "You are not authorized to view this patient profile");
                                        });

                        PatientEntity patient = patientRepo.findByAccountIdAndHospitalId(validAccountId, currentHospitalId)
                                        .orElseThrow(() -> new AccessDeniedException(
                                                        "Patient does not belong to the current hospital"));

                        Optional.of(role)
                                        .filter("PATIENT"::equals)
                                        .filter(r -> patient.getAccount() == null || !currentUser.getId().equals(patient.getAccount().getId()))
                                        .ifPresent(r -> {
                                                throw new AccessDeniedException(
                                                                "You are not authorized to view this patient profile");
                                        });

                        return patientMapper.toResponse(patient);
                } catch (AccessDeniedException | IllegalArgumentException e) {
                        throw e;
                } catch (Exception e) {
                        String msg = e.getMessage() != null && !e.getMessage().isBlank()
                                        ? e.getMessage()
                                        : "Unable to retrieve patient profile";
                        throw new RuntimeException(msg, e);
                }
        }

        @Override
        public long count() {
                try {
                        return patientRepo.count();
                } catch (Exception e) {
                        return 0L;
                }
        }

        @Override
        @Transactional(readOnly = true)
        public PatientEntity findPatientByAccountId(UUID accountId) {
                try {
                        return Optional.ofNullable(accountId)
                                        .flatMap(patientRepo::findByAccountId)
                                        .orElseThrow(() -> new IllegalArgumentException(
                                                        accountId == null ? "Account Id is required" : "Patient profile not found for this account"));
                } catch (IllegalArgumentException e) {
                        throw e;
                } catch (Exception e) {
                        String msg = e.getMessage() != null && !e.getMessage().isBlank()
                                        ? e.getMessage()
                                        : "Unable to find patient by account ID";
                        throw new RuntimeException(msg, e);
                }
        }

        @Override
        @Transactional(readOnly = true)
        public Optional<PatientEntity> findOptionalPatientByAccountId(UUID accountId) {
                try {
                        return Optional.ofNullable(accountId)
                                        .flatMap(patientRepo::findByAccountId);
                } catch (Exception e) {
                        return Optional.empty();
                }
        }

        @Override
        @Transactional(readOnly = true)
        public PatientEntity findPatientByAccountIdAndHospitalId(UUID accountId, UUID hospitalId) {
                try {
                        UUID validAccountId = Optional.ofNullable(accountId)
                                        .orElseThrow(() -> new IllegalArgumentException("Account Id is required"));
                        UUID validHospitalId = Optional.ofNullable(hospitalId)
                                        .orElseThrow(() -> new AccessDeniedException("Hospital context is required"));

                        return patientRepo.findByAccountIdAndHospitalId(validAccountId, validHospitalId)
                                        .orElseThrow(() -> new AccessDeniedException(
                                                        "Patient does not belong to the current hospital"));
                } catch (AccessDeniedException | IllegalArgumentException e) {
                        throw e;
                } catch (Exception e) {
                        String msg = e.getMessage() != null && !e.getMessage().isBlank()
                                        ? e.getMessage()
                                        : "Unable to find patient by account and hospital";
                        throw new RuntimeException(msg, e);
                }
        }

        @Override
        @Transactional(readOnly = true)
        public PatientResponse getMyProfile() {
                try {
                        UserEntity currentUser = requireCurrentUser();

                        Optional.of(getRole(currentUser))
                                        .filter("PATIENT"::equals)
                                        .orElseThrow(() -> new AccessDeniedException("Authenticated user is not a patient"));

                        PatientEntity patient = patientRepo.findByAccountId(currentUser.getId())
                                        .orElseThrow(() -> new IllegalArgumentException(
                                                        "Patient profile not found for this account"));

                        UUID patHospId = (patient.getHospital() != null) ? patient.getHospital().getId() : null;
                        if (patHospId == null) {
                                throw new AccessDeniedException("Patient profile has no hospital assigned");
                        }

                        UserEntity patientAcc = (patient.getAccount() != null) ? patient.getAccount() : null;
                        UUID patientUserId = (patientAcc != null) ? patientAcc.getId() : null;
                        if (patientUserId == null || currentUser == null || !patientUserId.equals(currentUser.getId())) {
                                throw new AccessDeniedException("You are not authorized to view this patient profile");
                        }

                        return patientMapper.toResponse(patient);
                } catch (AccessDeniedException | IllegalArgumentException e) {
                        throw e;
                } catch (Exception e) {
                        String msg = e.getMessage() != null && !e.getMessage().isBlank()
                                        ? e.getMessage()
                                        : "Unable to fetch current patient profile";
                        throw new RuntimeException(msg, e);
                }
        }

        private UserEntity requireCurrentUser() {
                return Optional.ofNullable(SecurityUtils.getCurrentUser())
                                .orElseThrow(() -> new AccessDeniedException("Authenticated user not found"));
        }

        private UUID requireCurrentHospital() {
                return Optional.ofNullable(tenantContextService.getCurrentUserHospitalId())
                                .orElseThrow(() -> new AccessDeniedException("Hospital context is required"));
        }

        private String getRole(UserEntity user) {
                String roleName = (user != null && user.getRole() != null && user.getRole().getRoleName() != null)
                                ? user.getRole().getRoleName().trim().toUpperCase(java.util.Locale.ROOT)
                                : null;
                if (roleName == null || roleName.isEmpty()) {
                        throw new AccessDeniedException("User role is not available");
                }
                return roleName;
        }


        @Override
        @Transactional(readOnly = true)
        public long countByHospitalId(UUID hospitalId) {
                return patientRepo.countByHospitalId(hospitalId);
        }
}