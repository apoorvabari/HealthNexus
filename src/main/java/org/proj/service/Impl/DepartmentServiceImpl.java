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
import org.proj.repository.DepartmentRepo;
import org.proj.repository.DoctorRepo;
import org.proj.service.DepartmentService;
import org.proj.service.HospitalService;
import org.proj.service.TenantContextService;

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
    private final HospitalService hospitalService;
    private final DepartmentMapper departmentMapper;
    private final TenantContextService tenantContextService;

    @Override
    @Transactional
    public DepartmentResponse createDepartment(DepartmentRequest request) {

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
    }

    @Override
    @Transactional(readOnly = true)
    public DepartmentResponse getDepartmentById(UUID id) {

        if (id == null) {
            throw new IllegalArgumentException(
                    "Department Id is required");
        }

        DepartmentEntity department = findDepartmentById(id);

        return departmentMapper.toResponse(department);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<DepartmentResponse> getAllDepartments(
            UUID hospitalId,
            String search,
            int page,
            int size) {

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
            if (!tenantContextService.hasCurrentUserHospitalAccess(hospitalId)) {
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

        List<DepartmentResponse> content =
                departmentPage.getContent()
                        .stream()
                        .map(departmentMapper::toResponse)
                        .toList();

        return new PageResponse<>(
                content,
                departmentPage.getNumber(),
                departmentPage.getSize(),
                departmentPage.getTotalElements(),
                departmentPage.getTotalPages(),
                departmentPage.isLast());
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

        List<DepartmentResponse> content = departmentPage.getContent()
                .stream()
                .map(departmentMapper::toResponse)
                .toList();

        return new PageResponse<>(
                content,
                departmentPage.getNumber(),
                departmentPage.getSize(),
                departmentPage.getTotalElements(),
                departmentPage.getTotalPages(),
                departmentPage.isLast());
    }

    @Override
    @Transactional
    public DepartmentResponse updateDepartment(
            UUID id,
            DepartmentRequest request) {

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
    }

    @Override
    @Transactional
    public void deleteDepartment(UUID id) {

        if (id == null) {
            throw new IllegalArgumentException(
                    "Department Id is required");
        }

        DepartmentEntity department = findDepartmentById(id);

        department.setStatus(
                DepartmentEntity.DepartmentStatus.INACTIVE);

        departmentRepo.save(department);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<DoctorResponse> getDoctorsByDepartment(
            UUID departmentId,
            int page,
            int size) {

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
        UUID hospitalId = department.getHospital().getId();

        Page<DoctorEntity> doctorPage =
                doctorRepo.findByDepartmentIdAndHospitalId(
                        departmentId,
                        hospitalId,
                        PageRequest.of(page, size));

        List<DoctorResponse> content =
                doctorPage.getContent()
                        .stream()
                        .map(doctor -> DoctorResponse.builder()

                                .id(doctor.getId())

                                .accountId(
                                        doctor.getAccount() != null
                                                ? doctor.getAccount().getId()
                                                : null)

                                .accountName(
                                        doctor.getAccount() != null
                                                ? doctor.getAccount().getFirstName()
                                                        + " "
                                                        + doctor.getAccount().getLastName()
                                                : null)

                                .hospitalId(
                                        doctor.getHospital() != null
                                                ? doctor.getHospital().getId()
                                                : null)

                                .hospitalName(
                                        doctor.getHospital() != null
                                                ? doctor.getHospital().getHospitalName()
                                                : null)

                                .departmentId(
                                        department.getId())

                                .departmentName(
                                        doctor.getDepartment() != null
                                                ? doctor.getDepartment().getDepartmentName()
                                                : null)

                                .specialization(
                                        doctor.getSpecialization())

                                .qualification(
                                        doctor.getQualification())

                                .experience(
                                        doctor.getExperience())

                                .consultationFee(
                                        doctor.getConsultationFee())

                                .licenseNumber(
                                        doctor.getLicenseNumber())

                                .status(
                                        doctor.getStatus())

                                .verificationStatus(
                                        doctor.getVerificationStatus())

                                .licenseVerified(
                                        doctor.getLicenseVerified())

                                .degreeVerified(
                                        doctor.getDegreeVerified())

                                .specializationVerified(
                                        doctor.getSpecializationVerified())

                                .verificationRemarks(
                                        doctor.getVerificationRemarks())

                                .verifiedBy(
                                        doctor.getVerifiedBy())

                                .verifiedAt(
                                        doctor.getVerifiedAt())

                                .message("Success")

                                .build())
                        .toList();

        return new PageResponse<>(
                content,
                doctorPage.getNumber(),
                doctorPage.getSize(),
                doctorPage.getTotalElements(),
                doctorPage.getTotalPages(),
                doctorPage.isLast());
    }

    @Override
    @Transactional(readOnly = true)
    public DepartmentAnalyticsResponse getDepartmentAnalytics(
            UUID departmentId) {

        if (departmentId == null) {
            throw new IllegalArgumentException(
                    "Department Id is required");
        }

        DepartmentEntity department = findDepartmentById(departmentId);
        UUID hospitalId = department.getHospital().getId();

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
    }

    @Override
    @Transactional(readOnly = true)
    public DepartmentEntity findDepartmentById(UUID departmentId) {

        if (departmentId == null) {
            throw new IllegalArgumentException(
                    "Department Id is required");
        }

        DepartmentEntity department = departmentRepo.findById(departmentId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Department not found"));

        if (department.getHospital() == null
                || !tenantContextService.hasCurrentUserHospitalAccess(department.getHospital().getId())) {
            throw new AccessDeniedException(
                    "Department does not belong to your hospital");
        }

        return department;
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
}
