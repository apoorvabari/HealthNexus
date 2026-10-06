package org.proj.service.impl;

import org.proj.dto.ReceptionistRequest;
import org.proj.dto.ReceptionistResponse;
import org.proj.entity.ReceptionistEntity;
import org.proj.entity.UserEntity;
import org.proj.entity.HospitalEntity;
import org.proj.entity.DepartmentEntity;
import org.proj.mapper.ReceptionistMapper;
import org.proj.repository.ReceptionistRepo;
import org.proj.repository.HospitalRepo;
import org.proj.repository.DepartmentRepo;
import org.proj.service.DepartmentService;
import org.proj.service.HospitalService;
import org.proj.service.NotificationService;
import org.proj.service.PatientService;
import org.proj.service.ReceptionistService;
import org.proj.service.TenantContextService;
import org.proj.service.UserService;
import org.proj.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReceptionistServiceImpl implements ReceptionistService {

    private final ReceptionistRepo receptionistRepo;
    private final HospitalRepo hospitalRepo;
    private final DepartmentRepo departmentRepo;
    private final UserService userService;
    private final HospitalService hospitalService;
    private final DepartmentService departmentService;
    private final ReceptionistMapper receptionistMapper;
    private final TenantContextService tenantContextService;
    private final PatientService patientService;
    private final NotificationService notificationService;
    private final org.proj.repository.PatientConsentRepo patientConsentRepo;

    @Override
    @Transactional
    public ReceptionistResponse createReceptionist(ReceptionistRequest request) {

        try {
            UserEntity currentUser = SecurityUtils.getCurrentUser();

            if (currentUser == null || currentUser.getId() == null) {
                throw new AccessDeniedException(
                        "Unable to determine authenticated user");
            }

            if (request == null) {
                throw new IllegalArgumentException(
                        "Receptionist request is required");
            }

            if (request.getAccountId() == null) {
                throw new IllegalArgumentException(
                        "Account ID is required");
            }

            if (request.getHospitalId() == null) {
                throw new IllegalArgumentException(
                        "Hospital ID is required");
            }

            if (request.getDepartmentId() == null) {
                throw new IllegalArgumentException(
                        "Department ID is required");
            }

            boolean currentUserIsReceptionist =
                    isReceptionist(currentUser);

            UUID targetHospitalId =
                    request.getHospitalId();

            /*
             * A receptionist can only create their own
             * receptionist profile.
             */
            if (currentUserIsReceptionist
                    && !currentUser.getId()
                    .equals(request.getAccountId())) {

                throw new AccessDeniedException(
                        "Receptionists can only create their own profile");
            }

            UserEntity account =
                    userService.findUserById(
                            request.getAccountId());

            if (account.getRole() == null
                    || !"RECEPTIONIST".equalsIgnoreCase(
                    account.getRole().getRoleName())) {

                throw new IllegalArgumentException(
                        "Account is not assigned RECEPTIONIST role.");
            }

            /*
             * Prevent duplicate receptionist profiles.
             */
            if (receptionistRepo.existsByAccountId(
                    request.getAccountId())) {

                throw new IllegalArgumentException(
                        "Receptionist profile already exists for this account");
            }

            HospitalEntity hospital;
            DepartmentEntity department;

            /*
             * FIRST-TIME RECEPTIONIST ONBOARDING
             *
             * At this point a receptionist profile does not exist.
             * Therefore tenantContextService cannot resolve the hospital
             * from the receptionist profile.
             *
             * The receptionist may select only an ACTIVE + APPROVED
             * public hospital.
             */
            if (currentUserIsReceptionist) {

                hospital = hospitalRepo.findById(targetHospitalId)
                                .orElseThrow(() ->
                                        new AccessDeniedException(
                                                "Selected hospital is not available for receptionist onboarding"));

                /*
                 * Department must belong to the exact selected hospital.
                 *
                 * We deliberately use the repository here instead of
                 * DepartmentService.findDepartmentById(), because that
                 * method requires an already established tenant.
                 */
                department =
                        departmentRepo
                                .findByIdAndHospitalId(
                                        request.getDepartmentId(),
                                        targetHospitalId)
                                .orElseThrow(() ->
                                        new AccessDeniedException(
                                                "Department does not belong to the selected hospital"));

            } else {

                /*
                 * ADMIN creation remains tenant restricted.
                 */
                UUID currentHospitalId =
                        requireCurrentHospital();

                if (!currentHospitalId.equals(
                        targetHospitalId)) {

                    throw new AccessDeniedException(
                            "You are not authorized to create a receptionist for another hospital");
                }

                hospital =
                        hospitalService.findHospitalById(
                                targetHospitalId);

                department =
                        departmentRepo
                                .findByIdAndHospitalId(
                                        request.getDepartmentId(),
                                        targetHospitalId)
                                .orElseThrow(() ->
                                        new AccessDeniedException(
                                                "Department does not belong to the selected hospital"));
            }

            /*
             * Employee code must be unique inside the selected hospital.
             */
            if (receptionistRepo
                    .existsByEmployeeCodeAndHospitalId(
                            request.getEmployeeCode(),
                            targetHospitalId)) {

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
                    receptionistRepo.save(
                            receptionist);

            ReceptionistResponse response =
                    receptionistMapper.toResponse(
                            savedReceptionist);

            response.setMessage(
                    "Receptionist profile created successfully");

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
            UserEntity currentUser = SecurityUtils.getCurrentUser();
            UUID currentHospitalId = tenantContextService.getCurrentUserHospitalId();

            if (currentHospitalId == null) {
                return List.of();
            }

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

            UserEntity currentUser = SecurityUtils.getCurrentUser();

            if (isReceptionist(currentUser)
                    && !accountId.equals(currentUser.getId())) {
                throw new AccessDeniedException(
                        "You are not authorized to view this receptionist's profile");
            }

            ReceptionistEntity receptionist =
                    receptionistRepo.findByAccountId(accountId)
                            .orElseThrow(() ->
                                    new EntityNotFoundException(
                                            "Receptionist profile not found for this account"));

            UUID currentHospitalId = tenantContextService.getCurrentUserHospitalId();
            if (currentHospitalId != null && receptionist.getHospital() != null
                    && !currentHospitalId.equals(receptionist.getHospital().getId())) {
                throw new AccessDeniedException(
                        "You are not authorized to view a receptionist from another hospital");
            }

            return receptionistMapper.toResponse(receptionist);

        } catch (AccessDeniedException | EntityNotFoundException e) {
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
                .role("PATIENT")
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

        org.proj.dto.PatientResponse createdPatient = patientService.createPatient(patientRequest);

        // Auto-grant APPOINTMENT_BOOKING consent for walk-in patient registration by receptionist
        org.proj.entity.PatientEntity patientEntity = patientService.findPatientByIdAndHospitalId(createdPatient.getId(), currentHospitalId);
        patientConsentRepo.save(
                org.proj.entity.PatientConsentEntity.builder()
                        .patient(patientEntity)
                        .consentType(org.proj.entity.PatientConsentEntity.ConsentType.APPOINTMENT_BOOKING)
                        .status(org.proj.entity.PatientConsentEntity.ConsentStatus.GRANTED)
                        .consentedAt(java.time.LocalDateTime.now())
                        .build()
        );

        return createdPatient;
    }
}
