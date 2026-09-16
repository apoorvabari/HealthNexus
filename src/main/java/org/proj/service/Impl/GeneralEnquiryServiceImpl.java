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
    }

    @Override
    @Transactional(readOnly = true)
    public List<GeneralEnquiryResponse> getHospitalEnquiries() {
        UUID hospitalId = tenantContextService.getCurrentUserHospitalId();
        if (hospitalId == null) {
            throw new AccessDeniedException("Tenant context required to view general enquiries");
        }

        return generalEnquiryRepo.findByHospitalIdOrderByCreatedAtDesc(hospitalId).stream()
                .map(e -> toResponse(e, null))
                .toList();
    }

    @Override
    @Transactional
    public GeneralEnquiryResponse updateEnquiryStatus(UUID enquiryId, GeneralEnquiryRequest request) {
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
