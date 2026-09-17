package org.proj.service.impl;

import org.proj.dto.ReceptionistRequest;
import org.proj.dto.ReceptionistResponse;
import org.proj.entity.ReceptionistEntity;
import org.proj.entity.UserEntity;
import org.proj.entity.HospitalEntity;
import org.proj.entity.DepartmentEntity;
import org.proj.mapper.ReceptionistMapper;
import org.proj.repository.ReceptionistRepo;
import org.proj.service.DepartmentService;
import org.proj.service.HospitalService;
import org.proj.service.NotificationService;
import org.proj.service.PatientService;
import org.proj.service.ReceptionistService;
import org.proj.service.TenantContextService;
import org.proj.service.UserService;
import org.proj.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReceptionistServiceImpl implements ReceptionistService {

    private final ReceptionistRepo receptionistRepo;
    private final UserService userService;
    private final HospitalService hospitalService;
    private final DepartmentService departmentService;
    private final ReceptionistMapper receptionistMapper;
    private final TenantContextService tenantContextService;
    private final PatientService patientService;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public ReceptionistResponse createReceptionist(ReceptionistRequest request) {
        try {
            UUID currentHospitalId = requireCurrentHospital();

            if (request == null) {
                throw new IllegalArgumentException("Receptionist request is required");
            }

            if (request.getAccountId() == null) {
                throw new IllegalArgumentException("Account ID is required");
            }

            if (request.getHospitalId() == null
                    || !currentHospitalId.equals(request.getHospitalId())) {
                throw new AccessDeniedException(
                        "You are not authorized to create a receptionist for another hospital");
            }

            UserEntity account = userService.findUserById(request.getAccountId());

            if (account.getRole() == null
                    || !"RECEPTIONIST".equalsIgnoreCase(account.getRole().getRoleName())) {
                throw new IllegalArgumentException("Account is not assigned RECEPTIONIST role.");
            }

            if (receptionistRepo.existsByAccountId(request.getAccountId())) {
                throw new IllegalArgumentException(
                        "Receptionist profile already exists for this account");
            }

            HospitalEntity hospital =
                    hospitalService.findHospitalById(currentHospitalId);

            DepartmentEntity department =
                    departmentService.findDepartmentById(request.getDepartmentId());

            if (department == null
                    || department.getHospital() == null
                    || !currentHospitalId.equals(department.getHospital().getId())) {
                throw new AccessDeniedException(
                        "Department does not belong to the current hospital");
            }

            if (receptionistRepo.existsByEmployeeCodeAndHospitalId(
                    request.getEmployeeCode(),
                    currentHospitalId)) {

                throw new IllegalArgumentException(
                        "Employee code already exists in this hospital");
            }

            ReceptionistEntity receptionist =
                    receptionistMapper.toEntity(
                            request,
                            account,
                            hospital,
                            department);

            ReceptionistEntity savedReceptionist =
                    receptionistRepo.save(receptionist);

            ReceptionistResponse response =
                    receptionistMapper.toResponse(savedReceptionist);

            response.setMessage("Receptionist profile created successfully");

            return response;

        } catch (AccessDeniedException e) {
            
            throw e;

        } catch (IllegalArgumentException e) {
            throw e;

        } catch (Exception e) {
            throw new RuntimeException(
                    "Unable to create receptionist profile.",
                    e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ReceptionistResponse getReceptionistById(UUID id) {

        try {
            if (id == null) {
                throw new IllegalArgumentException(
                        "Receptionist Id is required");
            }

            UUID currentHospitalId = requireCurrentHospital();

            ReceptionistEntity receptionist =
                    receptionistRepo.findByIdAndHospitalId(id, currentHospitalId)
                            .orElseThrow(() ->
                                    new AccessDeniedException(
                                            "You are not authorized to access this receptionist profile"));

            UserEntity currentUser = SecurityUtils.getCurrentUser();

            if (isReceptionist(currentUser)) {

                verifyOwnReceptionistProfile(
                        receptionist,
                        currentUser,
                        "view");
            }

            if (receptionist.getHospital() == null
                    || !currentHospitalId.equals(receptionist.getHospital().getId())) {
                throw new AccessDeniedException(
                        "You are not authorized to view a receptionist from another hospital");
            }

            return receptionistMapper.toResponse(receptionist);

        } catch (AccessDeniedException e) {
            
            throw e;

        } catch (IllegalArgumentException e) {
            throw e;

        } catch (Exception e) {
            throw new RuntimeException(
                    "Unable to fetch receptionist profile.",
                    e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReceptionistResponse> getAllReceptionists() {

        try {
            UUID currentHospitalId = requireCurrentHospital();
            UserEntity currentUser = SecurityUtils.getCurrentUser();

            List<ReceptionistEntity> receptionists;

            if (isReceptionist(currentUser)) {

                receptionists = receptionistRepo
                        .findByAccountId(currentUser.getId())
                        .filter(r -> r.getHospital() != null
                                && currentHospitalId.equals(r.getHospital().getId()))
                        .map(List::of)
                        .orElse(List.of());

            } else {
                receptionists = receptionistRepo.findByHospitalId(currentHospitalId);
            }

            return receptionists.stream()
                    .map(receptionistMapper::toResponse)
                    .toList();

        } catch (AccessDeniedException e) {
            throw e;

        } catch (Exception e) {
            throw new RuntimeException(
                    "Unable to fetch receptionist list.",
                    e);
        }
    }

    @Override
    @Transactional
    public ReceptionistResponse updateReceptionist(
            UUID id,
            ReceptionistRequest request) {

        try {
            if (id == null) {
                throw new IllegalArgumentException(
                        "Receptionist Id is required");
            }

            UUID currentHospitalId = requireCurrentHospital();

            ReceptionistEntity receptionist =
                    receptionistRepo.findByIdAndHospitalId(id, currentHospitalId)
                            .orElseThrow(() ->
                                    new AccessDeniedException(
                                            "You are not authorized to access this receptionist profile"));

            UserEntity currentUser = SecurityUtils.getCurrentUser();

            if (isReceptionist(currentUser)) {

                verifyOwnReceptionistProfile(
                        receptionist,
                        currentUser,
                        "update");

                if (request.getAccountId() != null
                        && !request.getAccountId().equals(currentUser.getId())) {

                    throw new AccessDeniedException(
                            "A receptionist cannot change the account "
                                    + "associated with the receptionist profile");
                }

                if (request.getHospitalId() != null
                        && !request.getHospitalId().equals(
                                receptionist.getHospital().getId())) {

                    throw new AccessDeniedException(
                            "A receptionist cannot change their hospital");
                }

                if (request.getDepartmentId() != null
                        && !request.getDepartmentId().equals(
                                receptionist.getDepartment().getId())) {

                    throw new AccessDeniedException(
                            "A receptionist cannot change their department");
                }

                if (request.getStatus() != null
                        && request.getStatus() != receptionist.getStatus()) {

                    throw new AccessDeniedException(
                            "A receptionist cannot change their status");
                }
            }

            UserEntity account = receptionist.getAccount();

            if (request.getAccountId() != null
                    && !request.getAccountId().equals(account.getId())) {

                account =
                        userService.findUserById(
                                request.getAccountId());

                if (account.getRole() == null
                        || !"RECEPTIONIST".equalsIgnoreCase(
                                account.getRole().getRoleName())) {

                    throw new IllegalArgumentException(
                            "Account is not assigned RECEPTIONIST role.");
                }

                if (receptionistRepo.existsByAccountIdAndIdNot(
                        request.getAccountId(),
                        id)) {

                    throw new IllegalArgumentException(
                            "Receptionist profile already exists for this account");
                }
            }

            HospitalEntity hospital = receptionist.getHospital();

            if (hospital == null || !currentHospitalId.equals(hospital.getId())) {
                throw new AccessDeniedException(
                        "Receptionist does not belong to the current hospital");
            }

            if (request.getHospitalId() != null
                    && !request.getHospitalId().equals(currentHospitalId)) {
                throw new AccessDeniedException(
                        "A receptionist profile cannot be moved to another hospital");
            }

            hospital = hospitalService.findHospitalById(currentHospitalId);

            DepartmentEntity department =
                    receptionist.getDepartment();

            if (request.getDepartmentId() != null
                    && !request.getDepartmentId()
                    .equals(department.getId())) {

                department =
                        departmentService.findDepartmentById(
                                request.getDepartmentId());

                if (department == null
                        || department.getHospital() == null
                        || !currentHospitalId.equals(department.getHospital().getId())) {
                    throw new AccessDeniedException(
                            "Department does not belong to the current hospital");
                }
            }

            String targetCode =
                    request.getEmployeeCode() != null
                            ? request.getEmployeeCode()
                            : receptionist.getEmployeeCode();

            UUID targetHospitalId = hospital.getId();

            if (receptionistRepo
                    .existsByEmployeeCodeAndHospitalIdAndIdNot(
                            targetCode,
                            targetHospitalId,
                            id)) {

                throw new IllegalArgumentException(
                        "Employee code already exists in this hospital");
            }

            receptionistMapper.updateEntity(
                    receptionist,
                    request,
                    account,
                    hospital,
                    department);

            ReceptionistEntity updatedReceptionist =
                    receptionistRepo.save(receptionist);

            ReceptionistResponse response =
                    receptionistMapper.toResponse(
                            updatedReceptionist);

            response.setMessage(
                    "Receptionist profile updated successfully");

            return response;

        } catch (AccessDeniedException e) {
            
            throw e;

        } catch (IllegalArgumentException e) {
            throw e;

        } catch (Exception e) {
            throw new RuntimeException(
                    "Unable to update receptionist profile.",
                    e);
        }
    }

    @Override
    @Transactional
    public void deleteReceptionist(UUID id) {

        try {
            if (id == null) {
                throw new IllegalArgumentException(
                        "Receptionist Id is required");
            }

            UUID currentHospitalId = requireCurrentHospital();

            ReceptionistEntity receptionist =
                    receptionistRepo.findByIdAndHospitalId(id, currentHospitalId)
                            .orElseThrow(() ->
                                    new AccessDeniedException(
                                            "You are not authorized to delete this receptionist profile"));

            receptionist.setStatus(
                    ReceptionistEntity.ReceptionistStatus.INACTIVE);

            receptionistRepo.save(receptionist);

        } catch (AccessDeniedException e) {
            throw e;

        } catch (IllegalArgumentException e) {
            throw e;

        } catch (Exception e) {
            throw new RuntimeException(
                    "Unable to delete receptionist profile.",
                    e);
        }
    }

    @Override
    public ReceptionistEntity findReceptionistById(
            UUID bookedByReceptionistId) {

        UUID currentHospitalId = requireCurrentHospital();

        return receptionistRepo.findByIdAndHospitalId(
                        bookedByReceptionistId, currentHospitalId)
                .orElseThrow(() ->
                        new AccessDeniedException(
                                "You are not authorized to access this receptionist profile"));
    }

    @Override
    @Transactional(readOnly = true)
    public ReceptionistResponse getReceptionistByAccountId(
            UUID accountId) {

        try {
            if (accountId == null) {
                throw new IllegalArgumentException(
                        "Account Id is required");
            }

            UUID currentHospitalId = requireCurrentHospital();

            ReceptionistEntity receptionist =
                    receptionistRepo.findByAccountId(accountId)
                            .orElseThrow(() ->
                                    new IllegalArgumentException(
                                            "Receptionist profile not found for this account"));

            if (receptionist.getHospital() == null
                    || !currentHospitalId.equals(receptionist.getHospital().getId())) {
                throw new AccessDeniedException(
                        "You are not authorized to view a receptionist from another hospital");
            }

            UserEntity currentUser =
                    SecurityUtils.getCurrentUser();

            if (isReceptionist(currentUser)
                    && !accountId.equals(currentUser.getId())) {

                throw new AccessDeniedException(
                        "You are not authorized to view this receptionist's profile");
            }

            return receptionistMapper.toResponse(receptionist);

        } catch (AccessDeniedException e) {
            
            throw e;

        } catch (IllegalArgumentException e) {
            throw e;

        } catch (Exception e) {
            throw new RuntimeException(
                    "Unable to fetch receptionist profile.",
                    e);
        }
    }

    private UUID requireCurrentHospital() {
        UUID hospitalId = tenantContextService.getCurrentUserHospitalId();

        if (hospitalId == null) {
            throw new AccessDeniedException(
                    "Tenant hospital context is required");
        }

        return hospitalId;
    }

    private boolean isReceptionist(UserEntity currentUser) {

        return currentUser != null
                && currentUser.getRole() != null
                && "RECEPTIONIST".equalsIgnoreCase(
                        currentUser.getRole().getRoleName());
    }

    private void verifyOwnReceptionistProfile(
            ReceptionistEntity receptionist,
            UserEntity currentUser,
            String operation) {

        if (currentUser == null) {
            throw new AccessDeniedException(
                    "Unable to determine authenticated user");
        }

        if (receptionist.getAccount() == null
                || receptionist.getAccount().getId() == null) {

            throw new AccessDeniedException(
                    "Receptionist profile has no valid account ownership");
        }

        if (!receptionist.getAccount()
                .getId()
                .equals(currentUser.getId())) {

            throw new AccessDeniedException(
                    "You are not authorized to "
                            + operation
                            + " this receptionist's profile");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.Optional<ReceptionistEntity> findReceptionistEntityByAccountId(UUID accountId) {
        if (accountId == null) {
            return java.util.Optional.empty();
        }

        UUID currentHospitalId = requireCurrentHospital();

        return receptionistRepo.findByAccountId(accountId)
                .filter(r -> r.getHospital() != null
                        && currentHospitalId.equals(r.getHospital().getId()));
    }

    @Override
    @Transactional
    public void callPatient(UUID patientId, String customMessage) {
        UUID receptionistHospitalId = tenantContextService.getCurrentUserHospitalId();

        if (receptionistHospitalId == null) {
            throw new AccessDeniedException("Hospital context is required to call a patient");
        }

        org.proj.entity.PatientEntity patient = patientService.findPatientById(patientId);

        if (patient.getHospital() == null
                || !receptionistHospitalId.equals(patient.getHospital().getId())) {
            throw new AccessDeniedException("You are not authorized to call a patient from another hospital");
        }

        String message = (customMessage != null && !customMessage.isBlank())
                ? customMessage
                : "Please proceed to the reception desk for your appointment.";

        notificationService.createNotification(
                patient.getAccount(),
                "Reception Counter Announcement",
                message,
                org.proj.entity.NotificationEntity.NotificationType.GENERAL
        );
    }

    @Override
    @Transactional
    public org.proj.dto.PatientResponse registerWalkInPatient(org.proj.dto.WalkInPatientRegistrationRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Registration request is required");
        }

        UUID currentHospitalId = requireCurrentHospital();

        org.proj.dto.RegisterRequest registerRequest = org.proj.dto.RegisterRequest.builder()
                .firstName(request.getFirstName())
                .middleName(request.getMiddleName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(request.getPassword())
                .phoneNumber(request.getPhoneNumber())
                .build();

        org.proj.dto.RegisterResponse userAccount = userService.register(registerRequest);
        if (userAccount == null || userAccount.getId() == null) {
            throw new IllegalStateException("Failed to create user account for walk-in patient");
        }

        org.proj.dto.PatientRequest patientRequest = org.proj.dto.PatientRequest.builder()
                .accountId(userAccount.getId())
                .hospitalId(currentHospitalId)
                .bloodGroup(request.getBloodGroup())
                .gender(request.getGender())
                .dateOfBirth(request.getDateOfBirth())
                .emergencyContactName(request.getEmergencyContactName())
                .emergencyContactPhone(request.getEmergencyContactPhone())
                .relationship(request.getRelationship())
                .address(request.getAddress())
                .city(request.getCity())
                .state(request.getState())
                .postalCode(request.getPostalCode())
                .status(org.proj.entity.PatientEntity.PatientStatus.ACTIVE)
                .build();

        return patientService.createPatient(patientRequest);
    }
}
