package org.proj.service.impl;

import org.proj.dto.HospitalRequest;
import org.proj.dto.HospitalResponse;
import org.proj.dto.PublicHospitalResponse;
import org.proj.dto.PageResponse;
import org.proj.entity.AdminEntity;
import org.proj.entity.HospitalEntity;
import org.proj.entity.UserEntity;
import org.proj.mapper.HospitalMapper;
import org.proj.repository.AdminRepo;
import org.proj.repository.HospitalRepo;
import org.proj.security.SecurityUtils;
import org.proj.service.HospitalService;
import org.proj.service.TenantContextService;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class HospitalServiceImpl implements HospitalService {

    private final HospitalRepo hospitalRepo;
    private final HospitalMapper hospitalMapper;
    private final TenantContextService tenantContextService;
    private final AdminRepo adminRepo;

    @Override
    @Transactional
    public HospitalResponse createHospital(HospitalRequest request) {

        try {
            
            UserEntity currentUser =
                    SecurityUtils.getCurrentUser();

            if (currentUser == null
                    || currentUser.getId() == null
                    || currentUser.getRole() == null
                    || currentUser.getRole().getRoleName() == null
                    || !"ADMIN".equalsIgnoreCase(
                            currentUser.getRole().getRoleName().trim())) {

                throw new AccessDeniedException(
                        "Only admins can create hospitals"
                );
            }

            if (request == null) {
                throw new IllegalArgumentException(
                        "Hospital request is required"
                );
            }

            if (request.getEmail() != null
                    && hospitalRepo.existsByEmail(
                    request.getEmail()
                            .trim()
                            .toLowerCase())) {

                throw new IllegalArgumentException(
                        "Hospital with this email already exists"
                );
            }

            if (request.getRegistrationNumber() != null
                    && hospitalRepo.existsByRegistrationNumber(
                    request.getRegistrationNumber())) {

                throw new IllegalArgumentException(
                        "Hospital with this registration number already exists"
                );
            }

            HospitalEntity hospital =
                    hospitalMapper.toEntity(request);

            hospital.setHospitalCode(
                    generateHospitalCode()
            );

            HospitalEntity savedHospital =
                    hospitalRepo.save(hospital);

            AdminEntity adminAssignment =
                    AdminEntity.builder()
                            .account(currentUser)
                            .hospital(savedHospital)
                            .status(AdminEntity.AdminStatus.ACTIVE)
                            .build();

            adminRepo.save(adminAssignment);

            HospitalResponse response =
                    hospitalMapper.toResponse(savedHospital);

            response.setMessage(
                    "Hospital created successfully"
            );

            return response;

        } catch (IllegalArgumentException e) {
            throw e;

        } catch (AccessDeniedException e) {
            throw e;

        } catch (Exception e) {
            throw new RuntimeException(
                    "Unable to create hospital.",
                    e
            );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<PublicHospitalResponse> getPublicHospitals() {
        return hospitalRepo.findPublicHospitals()
                .stream()
                .map(hospital -> PublicHospitalResponse.builder()
                        .id(hospital.getId())
                        .hospitalName(hospital.getHospitalName())
                        .hospitalCode(hospital.getHospitalCode())
                        .build())
                .toList();
    }

    @Override
    public HospitalResponse getHospitalById(UUID id) {

        try {
            if (id == null) {
                throw new IllegalArgumentException(
                        "Hospital Id is required"
                );
            }

            if (!tenantContextService
                    .hasCurrentUserHospitalAccess(id)) {

                throw new AccessDeniedException(
                        "You are not authorized to access this hospital"
                );
            }

            HospitalEntity hospital =
                    hospitalRepo.findByIdForTenant(id)
                            .orElseThrow(() ->
                                    new IllegalArgumentException(
                                            "Hospital not found"
                                    ));

            return hospitalMapper.toResponse(hospital);

        } catch (IllegalArgumentException e) {
            throw e;

        } catch (AccessDeniedException e) {
            throw e;

        } catch (Exception e) {
            throw new RuntimeException(
                    "Unable to fetch hospital.",
                    e
            );
        }
    }

    @Override
    public PageResponse<HospitalResponse> getAllHospitals(
            String search,
            int page,
            int size) {

        try {
            UserEntity currentUser =
                    SecurityUtils.getCurrentUser();

            if (currentUser == null
                    || currentUser.getId() == null
                    || currentUser.getRole() == null
                    || currentUser.getRole().getRoleName() == null) {

                throw new AccessDeniedException(
                        "User context could not be resolved"
                );
            }

            String roleName =
                    currentUser.getRole()
                            .getRoleName()
                            .trim()
                            .toUpperCase(Locale.ROOT);

            Page<HospitalEntity> hospPage;

            if ("ADMIN".equals(roleName)) {

                hospPage =
                        hospitalRepo.searchHospitalsByAdmin(
                                currentUser.getId(),
                                search,
                                PageRequest.of(page, size)
                        );

            } else {

                UUID currentHospitalId =
                        requireCurrentHospitalId();

                hospPage =
                        hospitalRepo.searchHospitalsForTenant(
                                currentHospitalId,
                                search,
                                PageRequest.of(page, size)
                        );
            }

            List<HospitalResponse> content =
                    hospPage.getContent()
                            .stream()
                            .map(hospitalMapper::toResponse)
                            .toList();

            return new PageResponse<>(
                    content,
                    hospPage.getNumber(),
                    hospPage.getSize(),
                    hospPage.getTotalElements(),
                    hospPage.getTotalPages(),
                    hospPage.isLast()
            );

        } catch (IllegalArgumentException e) {
            throw e;

        } catch (AccessDeniedException e) {
            throw e;

        } catch (Exception e) {
            throw new RuntimeException(
                    "Unable to fetch hospital list.",
                    e
            );
        }
    }

    @Override
    @Transactional
    public HospitalResponse updateHospital(
            UUID id,
            HospitalRequest request) {

        try {
            if (id == null) {
                throw new IllegalArgumentException(
                        "Hospital Id is required"
                );
            }

            if (!tenantContextService
                    .hasCurrentUserHospitalAccess(id)) {

                throw new AccessDeniedException(
                        "You are not authorized to update this hospital"
                );
            }

            HospitalEntity hospital =
                    hospitalRepo.findById(id)
                            .orElseThrow(() ->
                                    new IllegalArgumentException(
                                            "Hospital not found"
                                    ));

            if (request.getEmail() != null
                    && !request.getEmail()
                    .trim()
                    .toLowerCase()
                    .equals(
                            hospital.getEmail()
                    )) {

                if (hospitalRepo.existsByEmail(
                        request.getEmail()
                                .trim()
                                .toLowerCase())) {

                    throw new IllegalArgumentException(
                            "Hospital with this email already exists"
                    );
                }
            }

            if (request.getRegistrationNumber() != null
                    && !request.getRegistrationNumber()
                    .equals(
                            hospital.getRegistrationNumber()
                    )) {

                if (hospitalRepo.existsByRegistrationNumber(
                        request.getRegistrationNumber())) {

                    throw new IllegalArgumentException(
                            "Hospital with this registration number already exists"
                    );
                }
            }

            hospitalMapper.updateEntity(
                    hospital,
                    request
            );

            HospitalEntity updatedHospital =
                    hospitalRepo.save(hospital);

            HospitalResponse response =
                    hospitalMapper.toResponse(
                            updatedHospital
                    );

            response.setMessage(
                    "Hospital updated successfully"
            );

            return response;

        } catch (IllegalArgumentException e) {
            throw e;

        } catch (AccessDeniedException e) {
            throw e;

        } catch (Exception e) {
            throw new RuntimeException(
                    "Unable to update hospital.",
                    e
            );
        }
    }

    @Override
    @Transactional
    public void deleteHospital(UUID id) {

        try {
            if (id == null) {
                throw new IllegalArgumentException(
                        "Hospital Id is required"
                );
            }

            if (!tenantContextService
                    .hasCurrentUserHospitalAccess(id)) {

                throw new AccessDeniedException(
                        "You are not authorized to deactivate this hospital"
                );
            }

            HospitalEntity hospital =
                    findHospitalById(id);

            hospital.setStatus(
                    HospitalEntity.HospitalStatus.INACTIVE
            );

            hospitalRepo.save(hospital);

        } catch (IllegalArgumentException e) {
            throw e;

        } catch (AccessDeniedException e) {
            throw e;

        } catch (Exception e) {
            throw new RuntimeException(
                    "Unable to delete hospital.",
                    e
            );
        }
    }

    @Override
    public HospitalEntity findHospitalById(
            UUID hospitalId) {

        return hospitalRepo.findById(hospitalId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Hospital not found"
                        ));
    }

    @Override
    public void save(HospitalEntity hospital) {
        hospitalRepo.save(hospital);
    }

    @Override
    public long count() {
        return hospitalRepo.count();
    }

    @Override
    public long countByVerificationStatus(
            HospitalEntity.VerificationStatus verificationStatus) {

        return hospitalRepo.countByVerificationStatus(
                verificationStatus
        );
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

    private synchronized String generateHospitalCode() {

        String maxCode =
                hospitalRepo.findMaxHospitalCode();

        if (maxCode == null
                || maxCode.isBlank()) {

            return "HN001";
        }

        try {

            if (maxCode.startsWith("HN")
                    && maxCode.length() > 2) {

                String numberPart =
                        maxCode.substring(2);

                int number =
                        Integer.parseInt(numberPart);

                return String.format(
                        "HN%03d",
                        number + 1
                );
            }

        } catch (NumberFormatException e) {
            
        }

        long count =
                hospitalRepo.count();

        return String.format(
                "HN%03d",
                count + 1
        );
    }
}
