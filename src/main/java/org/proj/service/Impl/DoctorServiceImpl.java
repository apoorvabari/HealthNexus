package org.proj.service.impl;

import lombok.RequiredArgsConstructor;
import org.proj.dto.DoctorRequest;
import org.proj.dto.DoctorResponse;
import org.proj.dto.PageResponse;
import org.proj.entity.DepartmentEntity;
import org.proj.entity.DoctorEntity;
import org.proj.entity.HospitalEntity;
import org.proj.entity.UserEntity;
import org.proj.mapper.DoctorMapper;
import org.proj.repository.DoctorRepo;
import org.proj.security.SecurityUtils;
import org.proj.service.DepartmentService;
import org.proj.service.DoctorService;
import org.proj.service.HospitalService;
import org.proj.service.TenantContextService;
import org.proj.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DoctorServiceImpl implements DoctorService {

    private final DoctorRepo doctorRepo;
    private final UserService userService;
    private final HospitalService hospitalService;
    private final DepartmentService departmentService;
    private final DoctorMapper doctorMapper;
    private final TenantContextService tenantContextService;

    @Override
    @Transactional
    public DoctorResponse createDoctor(DoctorRequest request) {
        try {
            if (request == null) {
                throw new IllegalArgumentException("Doctor request is required");
            }

            UserEntity currentUser = requireCurrentUser();
            String role = getRole(currentUser);

            if (!"ADMIN".equals(role) && !"DOCTOR".equals(role)) {
                throw new AccessDeniedException(
                        "You are not authorized to create a doctor profile");
            }

            UUID currentHospitalId = requireCurrentHospital();

            if (request.getHospitalId() == null) {
                throw new IllegalArgumentException("Hospital ID is required");
            }

            if (!currentHospitalId.equals(request.getHospitalId())) {
                throw new AccessDeniedException(
                        "You cannot create a doctor for another hospital");
            }

            if (request.getAccountId() == null) {
                throw new IllegalArgumentException("Account ID is required");
            }

            if ("DOCTOR".equals(role)
                    && !currentUser.getId().equals(request.getAccountId())) {

                throw new AccessDeniedException(
                        "Doctors can create only their own doctor profile");
            }

            UserEntity account =
                    userService.findUserById(request.getAccountId());

            if (account == null
                    || account.getRole() == null
                    || !"DOCTOR".equalsIgnoreCase(
                    account.getRole().getRoleName())) {

                throw new IllegalArgumentException(
                        "Account is not assigned DOCTOR role.");
            }

            if (doctorRepo.existsByAccountId(request.getAccountId())) {
                throw new IllegalArgumentException(
                        "Doctor profile already exists for this account");
            }

            HospitalEntity hospital =
                    hospitalService.findHospitalById(currentHospitalId);

            if (hospital == null) {
                throw new IllegalArgumentException("Hospital not found");
            }

            if (request.getDepartmentId() == null) {
                throw new IllegalArgumentException(
                        "Department ID is required");
            }

            DepartmentEntity department =
                    departmentService.findDepartmentById(
                            request.getDepartmentId());

            validateDepartmentHospital(
                    department,
                    currentHospitalId);

            if (request.getLicenseNumber() == null
                    || request.getLicenseNumber().isBlank()) {

                throw new IllegalArgumentException(
                        "License number is required");
            }

            if (doctorRepo.existsByLicenseNumber(
                    request.getLicenseNumber())) {

                throw new IllegalArgumentException(
                        "Doctor with this license number already exists");
            }

            DoctorEntity doctor =
                    doctorMapper.toEntity(
                            request,
                            account,
                            hospital,
                            department);

            DoctorEntity savedDoctor =
                    doctorRepo.save(doctor);

            DoctorResponse response =
                    doctorMapper.toResponse(savedDoctor);

            response.setMessage(
                    "Doctor profile created successfully");

            return response;

        } catch (AccessDeniedException
                 | IllegalArgumentException e) {

            throw e;

        } catch (Exception e) {

            throw new RuntimeException(
                    "Unable to create doctor profile.",
                    e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<DoctorResponse> searchDoctorsForPatient(
            String search,
            String specialization,
            UUID hospitalId,
            UUID departmentId,
            BigDecimal maxFee,
            Integer minExperience,
            int page,
            int size) {

        try {

            if (page < 0) {
                throw new IllegalArgumentException(
                        "Page cannot be negative");
            }

            if (size < 1 || size > 50) {
                throw new IllegalArgumentException(
                        "Page size must be between 1 and 50");
            }

            if (maxFee != null
                    && maxFee.compareTo(BigDecimal.ZERO) < 0) {

                throw new IllegalArgumentException(
                        "Maximum fee cannot be negative");
            }

            if (minExperience != null
                    && minExperience < 0) {

                throw new IllegalArgumentException(
                        "Minimum experience cannot be negative");
            }

            UUID currentHospitalId =
                    requireCurrentHospital();

            Page<DoctorEntity> doctorPage =
                    doctorRepo.searchDoctorsForPatient(
                            search,
                            specialization,
                            currentHospitalId,
                            departmentId,
                            maxFee,
                            minExperience,
                            PageRequest.of(page, size));

            List<DoctorResponse> content =
                    doctorPage.getContent()
                            .stream()
                            .map(doctorMapper::toResponse)
                            .toList();

            return new PageResponse<>(
                    content,
                    doctorPage.getNumber(),
                    doctorPage.getSize(),
                    doctorPage.getTotalElements(),
                    doctorPage.getTotalPages(),
                    doctorPage.isLast());

        } catch (IllegalArgumentException e) {

            throw e;

        } catch (Exception e) {

            throw new RuntimeException(
                    "Unable to search doctors.",
                    e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<DoctorResponse> getNearbyDoctors(
            double latitude,
            double longitude,
            double radiusKm,
            String specialization,
            BigDecimal maxFee,
            Integer minExperience,
            int page,
            int size) {
        try {
            if (page < 0) {
                throw new IllegalArgumentException("Page cannot be negative");
            }
            if (size < 1 || size > 50) {
                throw new IllegalArgumentException("Page size must be between 1 and 50");
            }
            if (radiusKm <= 0) {
                throw new IllegalArgumentException("Radius must be greater than zero");
            }

            UUID currentHospitalId = requireCurrentHospital();
            HospitalEntity hospital = hospitalService.findHospitalById(currentHospitalId);

            if (hospital == null || hospital.getLatitude() == null || hospital.getLongitude() == null) {
                return new PageResponse<>(List.of(), page, size, 0, 0, true);
            }

            double distance = calculateHaversineDistanceKm(
                    latitude, longitude,
                    hospital.getLatitude(), hospital.getLongitude()
            );

            if (distance > radiusKm) {
                return new PageResponse<>(List.of(), page, size, 0, 0, true);
            }

            Page<DoctorEntity> doctorPage = doctorRepo.searchDoctorsForPatient(
                    null,
                    specialization,
                    currentHospitalId,
                    null,
                    maxFee,
                    minExperience,
                    PageRequest.of(page, size)
            );

            List<DoctorResponse> content = doctorPage.getContent()
                    .stream()
                    .map(doctor -> {
                        DoctorResponse resp = doctorMapper.toResponse(doctor);
                        resp.setDistanceKm(Math.round(distance * 100.0) / 100.0);
                        return resp;
                    })
                    .toList();

            return new PageResponse<>(
                    content,
                    doctorPage.getNumber(),
                    doctorPage.getSize(),
                    doctorPage.getTotalElements(),
                    doctorPage.getTotalPages(),
                    doctorPage.isLast()
            );
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Unable to search nearby doctors.", e);
        }
    }

    private double calculateHaversineDistanceKm(double lat1, double lon1, double lat2, double lon2) {
        final int EARTH_RADIUS_KM = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }

    @Override
    @Transactional(readOnly = true)
    public DoctorResponse getDoctorById(UUID id) {

        try {

            if (id == null) {
                throw new IllegalArgumentException(
                        "Doctor Id is required");
            }

            requireCurrentUser();

            UUID currentHospitalId =
                    requireCurrentHospital();

            DoctorEntity doctor =
                    doctorRepo.findByIdAndHospitalId(
                            id,
                            currentHospitalId)
                            .orElseThrow(() ->
                                    new AccessDeniedException(
                                            "You are not authorized to view a doctor from another hospital"));

            return doctorMapper.toResponse(doctor);

        } catch (AccessDeniedException
                 | IllegalArgumentException e) {

            throw e;

        } catch (Exception e) {

            throw new RuntimeException(
                    "Unable to fetch doctor profile.",
                    e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<DoctorResponse> getAllDoctors(
            String search,
            int page,
            int size,
            boolean isAdmin) {

        try {

            if (page < 0) {
                throw new IllegalArgumentException(
                        "Page number cannot be negative");
            }

            if (size < 1 || size > 50) {
                throw new IllegalArgumentException(
                        "Page size must be between 1 and 50");
            }

            UUID hospitalId =
                    tenantContextService.getCurrentUserHospitalId();

            if (hospitalId == null) {
                throw new AccessDeniedException(
                        "Tenant hospital context is required to list doctors");
            }

            Page<DoctorEntity> doctorPage;

            if (isAdmin) {

                doctorPage =
                        doctorRepo.searchDoctorsByHospital(
                                search,
                                hospitalId,
                                PageRequest.of(page, size));

            } else {

                doctorPage =
                        doctorRepo.searchApprovedAndActiveDoctorsByHospital(
                                search,
                                hospitalId,
                                PageRequest.of(page, size));
            }

            List<DoctorResponse> content =
                    doctorPage.getContent()
                            .stream()
                            .map(doctorMapper::toResponse)
                            .toList();

            return new PageResponse<>(
                    content,
                    doctorPage.getNumber(),
                    doctorPage.getSize(),
                    doctorPage.getTotalElements(),
                    doctorPage.getTotalPages(),
                    doctorPage.isLast());

        } catch (AccessDeniedException
                 | IllegalArgumentException e) {

            throw e;

        } catch (Exception e) {

            throw new RuntimeException(
                    "Unable to fetch doctor list.",
                    e);
        }
    }

    @Override
    @Transactional
    public DoctorResponse updateDoctor(
            UUID id,
            DoctorRequest request) {

        try {

            if (id == null) {
                throw new IllegalArgumentException(
                        "Doctor Id is required");
            }

            if (request == null) {
                throw new IllegalArgumentException(
                        "Doctor request is required");
            }

            UserEntity currentUser =
                    requireCurrentUser();

            String role =
                    getRole(currentUser);

            if (!"ADMIN".equals(role)
                    && !"DOCTOR".equals(role)) {

                throw new AccessDeniedException(
                        "You are not authorized to update a doctor profile");
            }

            UUID currentHospitalId =
                    requireCurrentHospital();

            DoctorEntity doctor =
                    doctorRepo.findByIdAndHospitalId(
                            id,
                            currentHospitalId)
                            .orElseThrow(() ->
                                    new AccessDeniedException(
                                            "You are not authorized to update a doctor from another hospital"));

            if ("DOCTOR".equals(role)
                    && (doctor.getAccount() == null
                    || !doctor.getAccount()
                    .getId()
                    .equals(currentUser.getId()))) {

                throw new AccessDeniedException(
                        "You are not authorized to update another doctor's profile");
            }

            if (request.getHospitalId() != null
                    && !currentHospitalId.equals(
                    request.getHospitalId())) {

                throw new AccessDeniedException(
                        "Doctor hospital cannot be changed");
            }

            UserEntity account =
                    doctor.getAccount();

            if (account == null) {
                throw new IllegalArgumentException(
                        "Doctor account is not configured");
            }

            if (request.getAccountId() != null
                    && !request.getAccountId()
                    .equals(account.getId())) {

                if ("DOCTOR".equals(role)) {

                    throw new AccessDeniedException(
                            "Doctors cannot change doctor account ownership");
                }

                UserEntity replacementAccount =
                        userService.findUserById(
                                request.getAccountId());

                if (replacementAccount == null
                        || replacementAccount.getRole() == null
                        || !"DOCTOR".equalsIgnoreCase(
                        replacementAccount
                                .getRole()
                                .getRoleName())) {

                    throw new IllegalArgumentException(
                            "Account is not assigned DOCTOR role.");
                }

                if (doctorRepo.existsByAccountIdAndIdNot(
                        request.getAccountId(),
                        id)) {

                    throw new IllegalArgumentException(
                            "Doctor profile already exists for this account");
                }

                account = replacementAccount;
            }

            HospitalEntity hospital =
                    doctor.getHospital();

            if (hospital == null
                    || !currentHospitalId.equals(
                    hospital.getId())) {

                throw new AccessDeniedException(
                        "Doctor does not belong to the current hospital");
            }

            DepartmentEntity department =
                    doctor.getDepartment();

            if (department == null) {

                throw new IllegalArgumentException(
                        "Doctor department is not configured");
            }

            if (request.getDepartmentId() != null
                    && !request.getDepartmentId()
                    .equals(department.getId())) {

                department =
                        departmentService.findDepartmentById(
                                request.getDepartmentId());

                validateDepartmentHospital(
                        department,
                        currentHospitalId);
            }

            if (request.getLicenseNumber() != null
                    && !request.getLicenseNumber()
                    .equals(doctor.getLicenseNumber())
                    && doctorRepo.existsByLicenseNumberAndIdNot(
                    request.getLicenseNumber(),
                    id)) {

                throw new IllegalArgumentException(
                        "Doctor with this license number already exists");
            }

            doctorMapper.updateEntity(
                    doctor,
                    request,
                    account,
                    hospital,
                    department);

            DoctorEntity updatedDoctor =
                    doctorRepo.save(doctor);

            DoctorResponse response =
                    doctorMapper.toResponse(updatedDoctor);

            response.setMessage(
                    "Doctor profile updated successfully");

            return response;

        } catch (AccessDeniedException
                 | IllegalArgumentException e) {

            throw e;

        } catch (Exception e) {

            throw new RuntimeException(
                    "Unable to update doctor profile.",
                    e);
        }
    }

    @Override
    @Transactional
    public void deleteDoctor(UUID id) {

        try {

            if (id == null) {
                throw new IllegalArgumentException(
                        "Doctor Id is required");
            }

            UUID currentHospitalId =
                    requireCurrentHospital();

            DoctorEntity doctor =
                    doctorRepo.findByIdAndHospitalId(
                            id,
                            currentHospitalId)
                            .orElseThrow(() ->
                                    new AccessDeniedException(
                                            "You are not authorized to delete a doctor from another hospital"));

            doctor.setStatus(
                    DoctorEntity.DoctorStatus.INACTIVE);

            doctorRepo.save(doctor);

        } catch (AccessDeniedException
                 | IllegalArgumentException e) {

            throw e;

        } catch (Exception e) {

            throw new RuntimeException(
                    "Unable to delete doctor profile.",
                    e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public DoctorEntity findDoctorById(UUID doctorId) {

        if (doctorId == null) {
            throw new IllegalArgumentException(
                    "Doctor Id is required");
        }

        return doctorRepo.findById(doctorId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Doctor not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public DoctorEntity findDoctorByIdAndHospitalId(UUID doctorId, UUID hospitalId) {

        if (doctorId == null) {
            throw new IllegalArgumentException(
                    "Doctor Id is required");
        }

        if (hospitalId == null) {
            throw new IllegalArgumentException(
                    "Hospital Id is required");
        }

        return doctorRepo.findByIdAndHospitalId(doctorId, hospitalId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Doctor not found for this hospital"));
    }

    @Override
    @Transactional(readOnly = true)
    public DoctorResponse getDoctorByAccountId(
            UUID accountId) {

        if (accountId == null) {
            throw new IllegalArgumentException(
                    "Account Id is required");
        }

        requireCurrentUser();

        UUID currentHospitalId =
                requireCurrentHospital();

        DoctorEntity doctor =
                doctorRepo.findByAccountId(accountId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Doctor profile not found for this account"));

        if (doctor.getHospital() == null
                || !currentHospitalId.equals(
                doctor.getHospital().getId())) {

            throw new AccessDeniedException(
                    "You are not authorized to view a doctor from another hospital");
        }

        return doctorMapper.toResponse(doctor);
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.Optional<DoctorEntity>
    findDoctorEntityByAccountId(UUID accountId) {

        return doctorRepo.findByAccountId(accountId);
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.Optional<DoctorEntity>
    findDoctorByAccountUsername(String username) {

        return doctorRepo.findByAccountEmail(username);
    }

    @Override
    public void save(DoctorEntity doctor) {
        doctorRepo.save(doctor);
    }

    @Override
    public long count() {
        return doctorRepo.count();
    }

    @Override
    public long countByVerificationStatus(
            DoctorEntity.VerificationStatus verificationStatus) {

        return doctorRepo.countByVerificationStatus(
                verificationStatus);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DoctorEntity> getDoctorsByDepartment(
            UUID departmentId,
            int page,
            int size) {

        return doctorRepo.findByDepartmentId(
                departmentId,
                PageRequest.of(page, size));
    }

    @Override
    @Transactional(readOnly = true)
    public long countDoctorsByDepartment(
            UUID departmentId) {

        return doctorRepo.countByDepartmentId(
                departmentId);
    }

    @Override
    @Transactional(readOnly = true)
    public long countDoctorsByDepartmentAndStatus(
            UUID departmentId,
            DoctorEntity.DoctorStatus status) {

        return doctorRepo.countByDepartmentIdAndStatus(
                departmentId,
                status);
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

    private void validateDepartmentHospital(
            DepartmentEntity department,
            UUID hospitalId) {

        if (department == null
                || department.getHospital() == null
                || department.getHospital().getId() == null
                || !hospitalId.equals(
                department.getHospital().getId())) {

            throw new AccessDeniedException(
                    "Department does not belong to the current hospital");
        }
    }
}
