package org.proj.service.impl;

import org.proj.dto.GeneralEnquiryRequest;
import org.proj.dto.GeneralEnquiryResponse;
import org.proj.entity.GeneralEnquiryEntity;
import org.proj.entity.GeneralEnquiryEntity.EnquiryStatus;
import org.proj.entity.HospitalEntity;
import org.proj.repository.GeneralEnquiryRepo;
import org.proj.service.GeneralEnquiryService;
import org.proj.service.HospitalService;
import org.proj.service.TenantContextService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GeneralEnquiryServiceImpl implements GeneralEnquiryService {

    private final GeneralEnquiryRepo generalEnquiryRepo;
    private final HospitalService hospitalService;
    private final TenantContextService tenantContextService;

    @Override
    @Transactional
    public GeneralEnquiryResponse createEnquiry(GeneralEnquiryRequest request) {
        try {
            if (request == null) {
                throw new IllegalArgumentException("General enquiry request cannot be null");
            }
            if (request.getInquirerName() == null || request.getInquirerName().trim().isEmpty()) {
                throw new IllegalArgumentException("Inquirer name is required");
            }
            if (request.getContactNumber() == null || request.getContactNumber().trim().isEmpty()) {
                throw new IllegalArgumentException("Contact number is required");
            }
            if (request.getEnquiryText() == null || request.getEnquiryText().trim().isEmpty()) {
                throw new IllegalArgumentException("Enquiry text is required");
            }

            UUID hospitalId = tenantContextService.getCurrentUserHospitalId();
            if (hospitalId == null) {
                throw new AccessDeniedException("Authenticated user must belong to a hospital to record general enquiries");
            }

            HospitalEntity hospital = hospitalService.findHospitalById(hospitalId);

            GeneralEnquiryEntity enquiry = GeneralEnquiryEntity.builder()
                    .hospital(hospital)
                    .inquirerName(request.getInquirerName().trim())
                    .contactNumber(request.getContactNumber().trim())
                    .enquiryText(request.getEnquiryText().trim())
                    .resolutionNotes(request.getResolutionNotes())
                    .status(EnquiryStatus.PENDING)
                    .build();

            GeneralEnquiryEntity saved = generalEnquiryRepo.save(enquiry);
            return toResponse(saved, "General enquiry created successfully");
        } catch (AccessDeniedException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            String msg = e.getMessage() != null && !e.getMessage().isBlank()
                    ? e.getMessage()
                    : "Unable to create general enquiry";
            throw new RuntimeException(msg, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<GeneralEnquiryResponse> getHospitalEnquiries() {
        try {
            UUID hospitalId = tenantContextService.getCurrentUserHospitalId();
            if (hospitalId == null) {
                throw new AccessDeniedException("Tenant context required to view general enquiries");
            }

            List<GeneralEnquiryEntity> enquiries = generalEnquiryRepo.findByHospitalIdOrderByCreatedAtDesc(hospitalId);
            return enquiries != null
                    ? enquiries.stream().map(e -> toResponse(e, null)).toList()
                    : List.of();
        } catch (AccessDeniedException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            String msg = e.getMessage() != null && !e.getMessage().isBlank()
                    ? e.getMessage()
                    : "Unable to fetch general enquiries";
            throw new RuntimeException(msg, e);
        }
    }

    @Override
    @Transactional
    public GeneralEnquiryResponse updateEnquiryStatus(UUID enquiryId, GeneralEnquiryRequest request) {
        try {
            if (enquiryId == null) {
                throw new IllegalArgumentException("Enquiry ID is required");
            }
            if (request == null) {
                throw new IllegalArgumentException("Update enquiry request cannot be null");
            }

            UUID hospitalId = tenantContextService.getCurrentUserHospitalId();
            if (hospitalId == null) {
                throw new AccessDeniedException("Tenant context required to update general enquiries");
            }

            GeneralEnquiryEntity enquiry = generalEnquiryRepo.findById(enquiryId)
                    .orElseThrow(() -> new IllegalArgumentException("Enquiry not found"));

            if (enquiry.getHospital() == null || enquiry.getHospital().getId() == null || !enquiry.getHospital().getId().equals(hospitalId)) {
                throw new AccessDeniedException("You are not authorized to update an enquiry for another hospital");
            }

            if (request.getStatus() != null && !request.getStatus().isBlank()) {
                enquiry.setStatus(EnquiryStatus.valueOf(request.getStatus().trim().toUpperCase()));
            }

            if (request.getResolutionNotes() != null) {
                enquiry.setResolutionNotes(request.getResolutionNotes().trim());
            }

            GeneralEnquiryEntity saved = generalEnquiryRepo.save(enquiry);
            return toResponse(saved, "General enquiry status updated successfully");
        } catch (AccessDeniedException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            String msg = e.getMessage() != null && !e.getMessage().isBlank()
                    ? e.getMessage()
                    : "Unable to update general enquiry";
            throw new RuntimeException(msg, e);
        }
    }


    private GeneralEnquiryResponse toResponse(GeneralEnquiryEntity entity, String message) {
        return GeneralEnquiryResponse.builder()
                .id(entity.getId())
                .hospitalId(entity.getHospital().getId())
                .hospitalName(entity.getHospital().getHospitalName())
                .inquirerName(entity.getInquirerName())
                .contactNumber(entity.getContactNumber())
                .enquiryText(entity.getEnquiryText())
                .resolutionNotes(entity.getResolutionNotes())
                .status(entity.getStatus().name())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .message(message)
                .build();
    }
}
