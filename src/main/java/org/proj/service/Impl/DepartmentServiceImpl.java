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

        UUID currentHospitalId = requireCurrentHospital();

        if (request.getHospitalId() == null) {
            throw new IllegalArgumentException("Hospital ID is required");
        }

        /*
         * Never trust the hospital ID supplied by the client.
         * ADMIN can create departments only inside their own hospital.
         */
        if (!currentHospitalId.equals(request.getHospitalId())) {
            throw new AccessDeniedException(
                    "You cannot create a department for another hospital");
        }

        if (departmentRepo.existsByDepartmentCodeAndHospitalId(
                request.getDepartmentCode(),
                currentHospitalId)) {

            throw new IllegalArgumentException(
                    "Department code already exists in this hospital");
        }

        HospitalEntity hospital =
                hospitalService.findHospitalById(currentHospitalId);

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

        UUID currentHospitalId = requireCurrentHospital();

        DepartmentEntity department =
                departmentRepo.findByIdAndHospitalId(
                        id,
                        currentHospitalId)
                        .orElseThrow(() ->
                                new AccessDeniedException(
                                        "Department does not belong to your hospital"));

        return departmentMapper.toResponse(department);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<DepartmentResponse> getAllDepartments(
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

        UUID currentHospitalId = requireCurrentHospital();

        Page<DepartmentEntity> departmentPage =
                departmentRepo.searchDepartmentsByHospital(
                        search,
                        currentHospitalId,
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

        /*
         * Prevent tenant switching.
         */
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

        /*
         * Keep the department attached to the existing tenant.
         */
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

        UUID currentHospitalId = requireCurrentHospital();

        DepartmentEntity department =
                departmentRepo.findByIdAndHospitalId(
                        id,
                        currentHospitalId)
                        .orElseThrow(() ->
                                new AccessDeniedException(
                                        "Department does not belong to your hospital"));

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

        UUID currentHospitalId = requireCurrentHospital();

        DepartmentEntity department =
                departmentRepo.findByIdAndHospitalId(
                        departmentId,
                        currentHospitalId)
                        .orElseThrow(() ->
                                new AccessDeniedException(
                                        "Department does not belong to your hospital"));

        Page<DoctorEntity> doctorPage =
                doctorRepo.findByDepartmentIdAndHospitalId(
                        departmentId,
                        currentHospitalId,
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

        UUID currentHospitalId = requireCurrentHospital();

        departmentRepo.findByIdAndHospitalId(
                departmentId,
                currentHospitalId)
                .orElseThrow(() ->
                        new AccessDeniedException(
                                "Department does not belong to your hospital"));

        return DepartmentAnalyticsResponse.builder()

                .totalDoctors(
                        doctorRepo.countByDepartmentIdAndHospitalId(
                                departmentId,
                                currentHospitalId))

                .activeDoctors(
                        doctorRepo.countByDepartmentIdAndHospitalIdAndStatus(
                                departmentId,
                                currentHospitalId,
                                DoctorEntity.DoctorStatus.ACTIVE))

                .inactiveDoctors(
                        doctorRepo.countByDepartmentIdAndHospitalIdAndStatus(
                                departmentId,
                                currentHospitalId,
                                DoctorEntity.DoctorStatus.INACTIVE))

                .suspendedDoctors(
                        doctorRepo.countByDepartmentIdAndHospitalIdAndStatus(
                                departmentId,
                                currentHospitalId,
                                DoctorEntity.DoctorStatus.SUSPENDED))

                .build();
    }

    /**
     * Used internally by Doctor, Appointment and Receptionist services.
     *
     * The returned department is always restricted to the
     * authenticated user's hospital.
     */
    @Override
    @Transactional(readOnly = true)
    public DepartmentEntity findDepartmentById(UUID departmentId) {

        if (departmentId == null) {
            throw new IllegalArgumentException(
                    "Department Id is required");
        }

        UUID currentHospitalId = requireCurrentHospital();

        return departmentRepo.findByIdAndHospitalId(
                departmentId,
                currentHospitalId)
                .orElseThrow(() ->
                        new AccessDeniedException(
                                "Department does not belong to your hospital"));
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