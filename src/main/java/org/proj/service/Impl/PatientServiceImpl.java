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
    public PatientResponse createPatient(
            PatientRequest request) {

        try {

            if (request == null) {
                throw new IllegalArgumentException(
                        "Patient request is required");
            }

            UserEntity currentUser =
                    requireCurrentUser();

            String role =
                    getRole(currentUser);

            if ("PATIENT".equals(role)) {
                request.setAccountId(
                        currentUser.getId());
            }

            if (request.getAccountId() == null) {
                throw new IllegalArgumentException(
                        "Account ID is required");
            }

            UserEntity account =
                    userService.findUserById(
                            request.getAccountId());

            if (account == null
                    || account.getRole() == null
                    || !"PATIENT".equalsIgnoreCase(
                    account.getRole().getRoleName())) {

                throw new IllegalArgumentException(
                        "Account is not assigned PATIENT role.");
            }

            if (patientRepo.existsByAccountId(
                    request.getAccountId())) {

                throw new IllegalArgumentException(
                        "Patient profile already exists for this account");
            }

            boolean staff =
                    "ADMIN".equals(role)
                            || "RECEPTIONIST".equals(role);

            UUID targetHospitalId =
                    request.getHospitalId();

            if (staff) {

                UUID staffHospital =
                        requireCurrentHospital();

                targetHospitalId =
                        staffHospital;

                request.setHospitalId(
                        staffHospital);

            } else if ("PATIENT".equals(role)) {

                if (targetHospitalId == null) {

                    throw new IllegalArgumentException(
                            "Hospital ID is required");
                }

            } else {

                throw new AccessDeniedException(
                        "You are not authorized to create a patient profile");
            }

            if (targetHospitalId == null) {
                throw new IllegalArgumentException(
                        "Hospital ID is required");
            }

            HospitalEntity hospital =
                    hospitalService.findHospitalById(
                            targetHospitalId);

            if (hospital == null) {
                throw new IllegalArgumentException(
                        "Hospital not found");
            }

            if (request.getPatientCode() == null
                    || request.getPatientCode()
                    .trim()
                    .isEmpty()) {

                String autoCode;

                do {

                    autoCode =
                            "PAT"
                                    + UUID.randomUUID()
                                    .toString()
                                    .substring(0, 6)
                                    .toUpperCase();

                } while (
                        patientRepo
                                .existsByPatientCodeAndHospitalId(
                                        autoCode,
                                        targetHospitalId));

                request.setPatientCode(autoCode);

            } else if (
                    patientRepo
                            .existsByPatientCodeAndHospitalId(
                                    request.getPatientCode(),
                                    targetHospitalId)) {

                throw new IllegalArgumentException(
                        "Patient code already exists in this hospital");
            }

            PatientEntity patient =
                    patientMapper.toEntity(
                            request,
                            account,
                            hospital);

            PatientEntity savedPatient =
                    patientRepo.save(patient);

            PatientResponse response =
                    patientMapper.toResponse(
                            savedPatient);

            response.setMessage(
                    "Patient profile created successfully");

            return response;

        } catch (AccessDeniedException
                 | IllegalArgumentException e) {

            throw e;

        } catch (Exception e) {

            throw new RuntimeException(
                    "Unable to create patient profile.",
                    e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PatientResponse getPatientById(
            UUID id) {

        try {

            if (id == null) {
                throw new IllegalArgumentException(
                        "Patient Id is required");
            }

            UserEntity currentUser =
                    requireCurrentUser();

            String role =
                    getRole(currentUser);

            UUID currentHospitalId =
                    requireCurrentHospital();

            PatientEntity patient =
                    patientRepo.findByIdAndHospitalId(
                            id,
                            currentHospitalId)
                            .orElseThrow(() ->
                                    new AccessDeniedException(
                                            "You are not authorized to view a patient from another hospital"));

            if ("PATIENT".equals(role)) {

                if (patient.getAccount() == null
                        || !currentUser.getId()
                        .equals(patient.getAccount().getId())) {

                    throw new AccessDeniedException(
                            "Patients can access only their own profile");
                }
            }

            return patientMapper.toResponse(
                    patient);

        } catch (AccessDeniedException
                 | IllegalArgumentException e) {

            throw e;

        } catch (Exception e) {

            throw new RuntimeException(
                    "Unable to fetch patient profile.",
                    e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<PatientResponse> getAllPatients() {

        try {

            UserEntity currentUser =
                    requireCurrentUser();

            String role =
                    getRole(currentUser);

            if ("PATIENT".equals(role)) {

                return patientRepo
                        .findByAccountId(
                                currentUser.getId())
                        .stream()
                        .map(patientMapper::toResponse)
                        .toList();
            }

            UUID hospitalId =
                    requireCurrentHospital();

            List<PatientEntity> patients =
                    patientRepo.findByHospitalId(
                            hospitalId);

            return patients.stream()
                    .map(patientMapper::toResponse)
                    .toList();

        } catch (AccessDeniedException
                 | IllegalArgumentException e) {

            throw e;

        } catch (Exception e) {

            throw new RuntimeException(
                    "Unable to fetch patient list.",
                    e);
        }
    }

    @Override
    @Transactional
    public PatientResponse updatePatient(
            UUID id,
            PatientRequest request) {

        try {

            if (id == null) {
                throw new IllegalArgumentException(
                        "Patient Id is required");
            }

            if (request == null) {
                throw new IllegalArgumentException(
                        "Patient request is required");
            }

            UserEntity currentUser =
                    requireCurrentUser();

            String role =
                    getRole(currentUser);

            UUID currentHospitalId =
                    requireCurrentHospital();

            PatientEntity patient =
                    patientRepo.findByIdAndHospitalId(
                            id,
                            currentHospitalId)
                            .orElseThrow(() ->
                                    new AccessDeniedException(
                                            "You are not authorized to update a patient from another hospital"));

            if ("PATIENT".equals(role)) {

                if (patient.getAccount() == null
                        || !patient.getAccount()
                        .getId()
                        .equals(currentUser.getId())) {

                    throw new AccessDeniedException(
                            "You are not authorized to update this patient profile");
                }

            } else if (!"ADMIN".equals(role)
                    && !"RECEPTIONIST".equals(role)) {

                throw new AccessDeniedException(
                        "You are not authorized to update this patient profile");
            }

            UserEntity account =
                    patient.getAccount();

            if (account == null) {
                throw new IllegalArgumentException(
                        "Patient account is not configured");
            }

            if (request.getAccountId() != null
                    && !request.getAccountId()
                    .equals(account.getId())) {

                if ("PATIENT".equals(role)) {

                    throw new AccessDeniedException(
                            "Patients cannot change account ownership");
                }

                UserEntity replacementAccount =
                        userService.findUserById(
                                request.getAccountId());

                if (replacementAccount == null
                        || replacementAccount.getRole() == null
                        || !"PATIENT".equalsIgnoreCase(
                        replacementAccount
                                .getRole()
                                .getRoleName())) {

                    throw new IllegalArgumentException(
                            "Account is not assigned PATIENT role.");
                }

                if (patientRepo.existsByAccountIdAndIdNot(
                        request.getAccountId(),
                        id)) {

                    throw new IllegalArgumentException(
                            "Patient profile already exists for this account");
                }

                account =
                        replacementAccount;
            }

            HospitalEntity hospital =
                    patient.getHospital();

            if (hospital == null
                    || !currentHospitalId.equals(
                    hospital.getId())) {

                throw new AccessDeniedException(
                        "Patient does not belong to the current hospital");
            }

            if (request.getHospitalId() != null
                    && !currentHospitalId.equals(
                    request.getHospitalId())) {

                throw new AccessDeniedException(
                        "Patient hospital cannot be changed");
            }

            if ("PATIENT".equals(role)) {

                if (request.getPatientCode() != null
                        && !request.getPatientCode()
                        .equals(patient.getPatientCode())) {

                    throw new AccessDeniedException(
                            "Patients cannot change patient code");
                }

                if (request.getStatus() != null
                        && !request.getStatus()
                        .equals(patient.getStatus())) {

                    throw new AccessDeniedException(
                            "Patients cannot change patient status");
                }
            }

            String targetCode =
                    request.getPatientCode() != null
                            ? request.getPatientCode()
                            : patient.getPatientCode();

            if (patientRepo
                    .existsByPatientCodeAndHospitalIdAndIdNot(
                            targetCode,
                            currentHospitalId,
                            id)) {

                throw new IllegalArgumentException(
                        "Patient code already exists in this hospital");
            }

            patientMapper.updateEntity(
                    patient,
                    request,
                    account,
                    hospital);

            PatientEntity updatedPatient =
                    patientRepo.save(patient);

            PatientResponse response =
                    patientMapper.toResponse(
                            updatedPatient);

            response.setMessage(
                    "Patient profile updated successfully");

            return response;

        } catch (AccessDeniedException
                 | IllegalArgumentException e) {

            throw e;

        } catch (Exception e) {

            throw new RuntimeException(
                    "Unable to update patient profile.",
                    e);
        }
    }

    @Override
    @Transactional
    public void deletePatient(UUID id) {

        try {

            if (id == null) {
                throw new IllegalArgumentException(
                        "Patient Id is required");
            }

            UUID currentHospitalId =
                    requireCurrentHospital();

            PatientEntity patient =
                    patientRepo.findByIdAndHospitalId(
                            id,
                            currentHospitalId)
                            .orElseThrow(() ->
                                    new AccessDeniedException(
                                            "You are not authorized to delete a patient from another hospital"));

            patient.setStatus(
                    PatientEntity.PatientStatus.INACTIVE);

            patientRepo.save(patient);

        } catch (AccessDeniedException
                 | IllegalArgumentException e) {

            throw e;

        } catch (Exception e) {

            throw new RuntimeException(
                    "Unable to delete patient profile.",
                    e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PatientEntity findPatientById(
            UUID patientId) {

        if (patientId == null) {
            throw new IllegalArgumentException(
                    "Patient Id is required");
        }

        return patientRepo.findById(patientId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Patient not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public PatientEntity findPatientByIdAndHospitalId(
            UUID patientId,
            UUID hospitalId) {

        if (patientId == null) {
            throw new IllegalArgumentException(
                    "Patient Id is required");
        }

        if (hospitalId == null) {
            throw new AccessDeniedException(
                    "Hospital context is required");
        }

        return patientRepo.findByIdAndHospitalId(
                        patientId,
                        hospitalId)
                .orElseThrow(() ->
                        new AccessDeniedException(
                                "Patient does not belong to the current hospital"));
    }

    @Override
    @Transactional(readOnly = true)
    public PatientResponse getPatientByAccountId(
            UUID accountId) {

        if (accountId == null) {
            throw new IllegalArgumentException(
                    "Account Id is required");
        }

        UserEntity currentUser =
                requireCurrentUser();

        String role =
                getRole(currentUser);

        UUID currentHospitalId =
                requireCurrentHospital();

        if ("PATIENT".equals(role)
                && !currentUser.getId()
                .equals(accountId)) {

            throw new AccessDeniedException(
                    "You are not authorized to view this patient profile");
        }

        PatientEntity patient =
                patientRepo.findByAccountIdAndHospitalId(
                        accountId,
                        currentHospitalId)
                        .orElseThrow(() ->
                                new AccessDeniedException(
                                        "Patient does not belong to the current hospital"));

        if ("PATIENT".equals(role)
                && (patient.getAccount() == null
                || !currentUser.getId()
                .equals(patient.getAccount().getId()))) {

            throw new AccessDeniedException(
                    "You are not authorized to view this patient profile");
        }

        return patientMapper.toResponse(
                patient);
    }

    @Override
    public long count() {
        return patientRepo.count();
    }

    @Override
    @Transactional(readOnly = true)
    public PatientEntity findPatientByAccountId(
            UUID accountId) {

        if (accountId == null) {
            throw new IllegalArgumentException(
                    "Account Id is required");
        }

        return patientRepo.findByAccountId(accountId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Patient profile not found for this account"));
    }

    @Override
    @Transactional(readOnly = true)
    public PatientEntity findPatientByAccountIdAndHospitalId(
            UUID accountId,
            UUID hospitalId) {

        if (accountId == null) {
            throw new IllegalArgumentException(
                    "Account Id is required");
        }

        if (hospitalId == null) {
            throw new AccessDeniedException(
                    "Hospital context is required");
        }

        return patientRepo.findByAccountIdAndHospitalId(
                        accountId,
                        hospitalId)
                .orElseThrow(() ->
                        new AccessDeniedException(
                                "Patient does not belong to the current hospital"));
    }

    @Override
    @Transactional(readOnly = true)
    public PatientResponse getMyProfile() {

        UserEntity currentUser =
                requireCurrentUser();

        if (!"PATIENT".equals(
                getRole(currentUser))) {

            throw new AccessDeniedException(
                    "Authenticated patient not found");
        }

        UUID currentHospitalId =
                requireCurrentHospital();

        PatientEntity patient =
                patientRepo.findByAccountIdAndHospitalId(
                        currentUser.getId(),
                        currentHospitalId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Patient profile not found for this account"));

        return patientMapper.toResponse(
                patient);
    }

    private UserEntity requireCurrentUser() {

        UserEntity currentUser =
                SecurityUtils.getCurrentUser();

        if (currentUser == null
                || currentUser.getId() == null) {

            throw new AccessDeniedException(
                    "Authenticated user not found");
        }

        return currentUser;
    }

    private String getRole(UserEntity user) {

        if (user == null
                || user.getRole() == null
                || user.getRole().getRoleName() == null) {

            throw new AccessDeniedException(
                    "Authenticated user role is missing");
        }

        return user.getRole()
                .getRoleName()
                .trim()
                .toUpperCase();
    }

    private UUID requireCurrentHospital() {

        UUID hospitalId =
                tenantContextService
                        .getCurrentUserHospitalId();

        if (hospitalId == null) {

            throw new AccessDeniedException(
                    "Hospital context is required");
        }

        return hospitalId;
    }
}
