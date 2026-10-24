package org.proj.service.impl;

import java.util.List;
import java.util.UUID;

import org.proj.dto.DepartmentAnalyticsResponse;
import org.proj.dto.DepartmentRequest;
import org.proj.dto.DepartmentResponse;
import org.proj.dto.DoctorResponse;
import org.proj.dto.PageResponse;
import org.proj.entity.DepartmentEntity;
import org.proj.entity.DoctorEntity;
import org.proj.entity.HospitalEntity;
import org.proj.mapper.DepartmentMapper;
import org.proj.repository.AdminRepo;
import org.proj.repository.DepartmentRepo;
import org.proj.repository.DoctorRepo;
import org.proj.repository.HospitalRepo;
import org.proj.service.DepartmentService;
import org.proj.service.HospitalService;
import org.proj.service.TenantContextService;
import org.proj.security.SecurityUtils;
import org.proj.entity.UserEntity;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentRepo departmentRepo;
    private final DoctorRepo doctorRepo;
    private final HospitalRepo hospitalRepo;
    private final AdminRepo adminRepo;
    private final HospitalService hospitalService;
    private final DepartmentMapper departmentMapper;
    private final TenantContextService tenantContextService;

    @Override
    @Transactional
    public DepartmentResponse createDepartment(DepartmentRequest request) {
        try {
            if (request == null) {
                throw new IllegalArgumentException("Department request is required");
            }

            if (request.getHospitalId() == null) {
                throw new IllegalArgumentException("Hospital ID is required");
            }

            UUID targetHospitalId = request.getHospitalId();

            if (!tenantContextService.hasCurrentUserHospitalAccess(targetHospitalId)) {
                throw new AccessDeniedException(
                        "You cannot create a department for another hospital");
            }

            if (departmentRepo.existsByDepartmentCodeAndHospitalId(
                    request.getDepartmentCode(),
                    targetHospitalId)) {

                throw new IllegalArgumentException(
                        "Department code already exists in this hospital");
            }

            HospitalEntity hospital =
                    hospitalService.findHospitalById(targetHospitalId);

            DepartmentEntity department =
                    departmentMapper.toEntity(request, hospital);

            DepartmentEntity savedDepartment =
                    departmentRepo.save(department);

            DepartmentResponse response =
                    departmentMapper.toResponse(savedDepartment);

            response.setMessage("Department created successfully");

            return response;
        } catch (AccessDeniedException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            String msg = e.getMessage() != null && !e.getMessage().isBlank()
                    ? e.getMessage()
                    : "Unable to create department";
            throw new RuntimeException(msg, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public DepartmentResponse getDepartmentById(UUID id) {
        try {
            if (id == null) {
                throw new IllegalArgumentException(
                        "Department Id is required");
            }

            DepartmentEntity department = findDepartmentById(id);

            return departmentMapper.toResponse(department);
        } catch (AccessDeniedException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            String msg = e.getMessage() != null && !e.getMessage().isBlank()
                    ? e.getMessage()
                    : "Unable to fetch department";
            throw new RuntimeException(msg, e);
        }
    }


    @Override
    @Transactional(readOnly = true)
    public PageResponse<DepartmentResponse> getAllDepartments(
            UUID hospitalId,
            String search,
            int page,
            int size) {
        try {
            if (page < 0) {
                throw new IllegalArgumentException(
                        "Page number cannot be negative");
            }

            if (size <= 0) {
                throw new IllegalArgumentException(
                        "Page size must be greater than zero");
            }

            List<UUID> hospitalIds;
            if (hospitalId != null) {
                UserEntity currentUser = SecurityUtils.getCurrentUser();
                boolean isPatient = currentUser != null && currentUser.getRole() != null && "PATIENT".equalsIgnoreCase(currentUser.getRole().getRoleName());
                if (!isPatient && !tenantContextService.hasCurrentUserHospitalAccess(hospitalId)) {
                    throw new AccessDeniedException(
                            "You cannot access departments for another hospital");
                }
                hospitalIds = List.of(hospitalId);
            } else {
                hospitalIds = tenantContextService.getCurrentUserHospitalIds();
            }

            if (hospitalIds == null || hospitalIds.isEmpty()) {
                return new PageResponse<>(
                        List.of(),
                        page,
                        size,
                        0,
                        0,
                        true);
            }

            Page<DepartmentEntity> departmentPage =
                    departmentRepo.searchDepartmentsByHospitalIds(
                            search,
                            hospitalIds,
                            PageRequest.of(page, size));

            List<DepartmentResponse> content = departmentPage != null
                    ? departmentPage.getContent().stream().map(departmentMapper::toResponse).toList()
                    : List.of();

            return new PageResponse<>(
                    content,
                    departmentPage != null ? departmentPage.getNumber() : page,
                    departmentPage != null ? departmentPage.getSize() : size,
                    departmentPage != null ? departmentPage.getTotalElements() : 0L,
                    departmentPage != null ? departmentPage.getTotalPages() : 0,
                    departmentPage != null ? departmentPage.isLast() : true);
        } catch (AccessDeniedException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            String msg = e.getMessage() != null && !e.getMessage().isBlank()
                    ? e.getMessage()
                    : "Unable to retrieve departments";
            throw new RuntimeException(msg, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<DepartmentResponse> getAllDepartments(
            String search,
            int page,
            int size) {
        return getAllDepartments(null, search, page, size);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<DepartmentResponse> getPublicDepartments(
            UUID hospitalId,
            String search,
            int page,
            int size) {
        try {
            if (hospitalId == null) {
                throw new IllegalArgumentException("Hospital ID is required");
            }
            if (page < 0) {
                throw new IllegalArgumentException("Page number cannot be negative");
            }
            if (size <= 0) {
                throw new IllegalArgumentException("Page size must be greater than zero");
            }

            try {
                hospitalService.findPublicHospitalById(hospitalId);
            } catch (Exception e) {
                throw new AccessDeniedException(
                        "Selected hospital is not available for public onboarding");
            }

            Page<DepartmentEntity> departmentPage =
                    departmentRepo.searchPublicDepartmentsByHospital(
                            search,
                            hospitalId,
                            PageRequest.of(page, size));

            List<DepartmentResponse> content = departmentPage != null
                    ? departmentPage.getContent().stream().map(departmentMapper::toResponse).toList()
                    : List.of();

            return new PageResponse<>(
                    content,
                    departmentPage != null ? departmentPage.getNumber() : page,
                    departmentPage != null ? departmentPage.getSize() : size,
                    departmentPage != null ? departmentPage.getTotalElements() : 0L,
                    departmentPage != null ? departmentPage.getTotalPages() : 0,
                    departmentPage != null ? departmentPage.isLast() : true);
        } catch (AccessDeniedException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            String msg = e.getMessage() != null && !e.getMessage().isBlank()
                    ? e.getMessage()
                    : "Unable to retrieve public departments";
            throw new RuntimeException(msg, e);
        }
    }

    @Override
    @Transactional
    public DepartmentResponse updateDepartment(
            UUID id,
            DepartmentRequest request) {
        try {
            if (id == null) {
                throw new IllegalArgumentException(
                        "Department Id is required");
            }

            if (request == null) {
                throw new IllegalArgumentException(
                        "Department request is required");
            }

            UUID currentHospitalId = requireCurrentHospital();

            DepartmentEntity department =
                    departmentRepo.findByIdAndHospitalId(
                            id,
                            currentHospitalId)
                            .orElseThrow(() ->
                                    new AccessDeniedException(
                                            "Department does not belong to your hospital"));

            HospitalEntity hospital = department.getHospital();

            if (hospital == null) {
                throw new IllegalArgumentException(
                        "Department is not associated with a hospital");
            }

            if (request.getHospitalId() != null
                    && !currentHospitalId.equals(request.getHospitalId())) {

                throw new AccessDeniedException(
                        "Department hospital cannot be changed");
            }

            String targetCode =
                    request.getDepartmentCode() != null
                            ? request.getDepartmentCode()
                            : department.getDepartmentCode();

            if (departmentRepo.existsByDepartmentCodeAndHospitalIdAndIdNot(
                    targetCode,
                    currentHospitalId,
                    id)) {

                throw new IllegalArgumentException(
                        "Department code already exists in this hospital");
            }

            departmentMapper.updateEntity(
                    department,
                    request,
                    hospital);

            DepartmentEntity updatedDepartment =
                    departmentRepo.save(department);

            DepartmentResponse response =
                    departmentMapper.toResponse(updatedDepartment);

            response.setMessage(
                    "Department updated successfully");

            return response;
        } catch (AccessDeniedException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            String msg = e.getMessage() != null && !e.getMessage().isBlank()
                    ? e.getMessage()
                    : "Unable to update department";
            throw new RuntimeException(msg, e);
        }
    }

    @Override
    @Transactional
    public void deleteDepartment(UUID id) {
        try {
            if (id == null) {
                throw new IllegalArgumentException(
                        "Department Id is required");
            }

            DepartmentEntity department = findDepartmentById(id);

            department.setStatus(
                    DepartmentEntity.DepartmentStatus.INACTIVE);

            departmentRepo.save(department);
        } catch (AccessDeniedException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            String msg = e.getMessage() != null && !e.getMessage().isBlank()
                    ? e.getMessage()
                    : "Unable to delete department";
            throw new RuntimeException(msg, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<DoctorResponse> getDoctorsByDepartment(
            UUID departmentId,
            int page,
            int size) {
        try {
            if (departmentId == null) {
                throw new IllegalArgumentException(
                        "Department Id is required");
            }

            if (page < 0) {
                throw new IllegalArgumentException(
                        "Page number cannot be negative");
            }

            if (size <= 0) {
                throw new IllegalArgumentException(
                        "Page size must be greater than zero");
            }

            DepartmentEntity department = findDepartmentById(departmentId);
            UUID hospitalId = department.getHospital() != null ? department.getHospital().getId() : null;

            Page<DoctorEntity> doctorPage =
                    doctorRepo.findByDepartmentIdAndHospitalId(
                            departmentId,
                            hospitalId,
                            PageRequest.of(page, size));

            List<DoctorResponse> content = doctorPage != null
                    ? doctorPage.getContent().stream().map(doctor -> DoctorResponse.builder()
                            .id(doctor.getId())
                            .accountId(doctor.getAccount() != null ? doctor.getAccount().getId() : null)
                            .accountName(doctor.getAccount() != null
                                    ? doctor.getAccount().getFirstName() + " " + doctor.getAccount().getLastName()
                                    : null)
                            .hospitalId(doctor.getHospital() != null ? doctor.getHospital().getId() : null)
                            .hospitalName(doctor.getHospital() != null ? doctor.getHospital().getHospitalName() : null)
                            .departmentId(department.getId())
                            .departmentName(doctor.getDepartment() != null ? doctor.getDepartment().getDepartmentName() : null)
                            .specialization(doctor.getSpecialization())
                            .qualification(doctor.getQualification())
                            .experience(doctor.getExperience())
                            .consultationFee(doctor.getConsultationFee())
                            .licenseNumber(doctor.getLicenseNumber())
                            .status(doctor.getStatus())
                            .verificationStatus(doctor.getVerificationStatus())
                            .licenseVerified(doctor.getLicenseVerified())
                            .degreeVerified(doctor.getDegreeVerified())
                            .specializationVerified(doctor.getSpecializationVerified())
                            .verificationRemarks(doctor.getVerificationRemarks())
                            .verifiedBy(doctor.getVerifiedBy())
                            .verifiedAt(doctor.getVerifiedAt())
                            .message("Success")
                            .build()).toList()
                    : List.of();

            return new PageResponse<>(
                    content,
                    doctorPage != null ? doctorPage.getNumber() : page,
                    doctorPage != null ? doctorPage.getSize() : size,
                    doctorPage != null ? doctorPage.getTotalElements() : 0L,
                    doctorPage != null ? doctorPage.getTotalPages() : 0,
                    doctorPage != null ? doctorPage.isLast() : true);
        } catch (AccessDeniedException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            String msg = e.getMessage() != null && !e.getMessage().isBlank()
                    ? e.getMessage()
                    : "Unable to fetch doctors by department";
            throw new RuntimeException(msg, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public DepartmentAnalyticsResponse getDepartmentAnalytics(
            UUID departmentId) {
        try {
            if (departmentId == null) {
                throw new IllegalArgumentException(
                        "Department Id is required");
            }

            DepartmentEntity department = findDepartmentById(departmentId);
            UUID hospitalId = department.getHospital() != null ? department.getHospital().getId() : null;

            return DepartmentAnalyticsResponse.builder()
                    .totalDoctors(
                            doctorRepo.countByDepartmentIdAndHospitalId(
                                    departmentId,
                                    hospitalId))
                    .activeDoctors(
                            doctorRepo.countByDepartmentIdAndHospitalIdAndStatus(
                                    departmentId,
                                    hospitalId,
                                    DoctorEntity.DoctorStatus.ACTIVE))
                    .inactiveDoctors(
                            doctorRepo.countByDepartmentIdAndHospitalIdAndStatus(
                                    departmentId,
                                    hospitalId,
                                    DoctorEntity.DoctorStatus.INACTIVE))
                    .suspendedDoctors(
                            doctorRepo.countByDepartmentIdAndHospitalIdAndStatus(
                                    departmentId,
                                    hospitalId,
                                    DoctorEntity.DoctorStatus.SUSPENDED))
                    .build();
        } catch (AccessDeniedException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            String msg = e.getMessage() != null && !e.getMessage().isBlank()
                    ? e.getMessage()
                    : "Unable to fetch department analytics";
            throw new RuntimeException(msg, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public DepartmentEntity findDepartmentById(UUID departmentId) {
        try {
            if (departmentId == null) {
                throw new IllegalArgumentException(
                        "Department Id is required");
            }

            DepartmentEntity department = departmentRepo.findById(departmentId)
                    .orElseThrow(() ->
                            new IllegalArgumentException(
                                    "Department not found"));

            boolean isPatient = SecurityUtils.isPatient();
            if (!isPatient && (department.getHospital() == null
                    || !tenantContextService.hasCurrentUserHospitalAccess(department.getHospital().getId()))) {
                throw new AccessDeniedException(
                        "Department does not belong to your hospital");
            }

            return department;
        } catch (AccessDeniedException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            String msg = e.getMessage() != null && !e.getMessage().isBlank()
                    ? e.getMessage()
                    : "Unable to find department";
            throw new RuntimeException(msg, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public DepartmentEntity findDepartmentByIdAndHospitalId(UUID departmentId, UUID hospitalId) {
        if (departmentId == null) {
            throw new IllegalArgumentException("Department Id is required");
        }
        if (hospitalId == null) {
            throw new IllegalArgumentException("Hospital Id is required");
        }
        return departmentRepo.findByIdAndHospitalId(departmentId, hospitalId)
                .orElseThrow(() -> new AccessDeniedException("Department does not belong to the selected hospital"));
    }

    private UUID requireCurrentHospital() {
        UUID hospitalId =
                tenantContextService.getCurrentUserHospitalId();

        if (hospitalId == null) {
            throw new AccessDeniedException(
                    "Unable to determine your hospital");
        }

        return hospitalId;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DepartmentResponse> getDepartmentsByHospital(UUID hospitalId) {
        try {
            if (hospitalId == null) {
                throw new IllegalArgumentException("Hospital ID is required");
            }
            if (!hospitalRepo.existsById(hospitalId)) {
                throw new IllegalArgumentException("Hospital not found with id: " + hospitalId);
            }

            UserEntity currentUser = SecurityUtils.getCurrentUser();

            if (currentUser != null && currentUser.getRole() != null) {
                String roleName = currentUser.getRole().getRoleName();
                if ("ADMIN".equalsIgnoreCase(roleName) || "ROLE_ADMIN".equalsIgnoreCase(roleName)) {
                    boolean ownsHospital = adminRepo.existsByAccountIdAndHospitalId(currentUser.getId(), hospitalId);
                    if (!ownsHospital) {
                        throw new AccessDeniedException("You cannot access departments for another admin's hospital");
                    }
                } else if ("DOCTOR".equalsIgnoreCase(roleName) || "RECEPTIONIST".equalsIgnoreCase(roleName)) {
                    if (!tenantContextService.hasCurrentUserHospitalAccess(hospitalId)) {
                        throw new AccessDeniedException("You cannot access departments for another hospital");
                    }
                }
                // PATIENT is allowed to view departments of any hospital
            }

            List<DepartmentEntity> departments = departmentRepo.findByHospitalId(hospitalId);
            return departments != null
                    ? departments.stream().map(departmentMapper::toResponse).toList()
                    : List.of();
        } catch (AccessDeniedException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            String msg = e.getMessage() != null && !e.getMessage().isBlank()
                    ? e.getMessage()
                    : "Unable to retrieve departments for hospital";
            throw new RuntimeException(msg, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.Optional<DepartmentEntity> findFirstDepartment() {
        try {
            return departmentRepo.findAll().stream().findFirst();
        } catch (Exception e) {
            return java.util.Optional.empty();
        }
    }
}

