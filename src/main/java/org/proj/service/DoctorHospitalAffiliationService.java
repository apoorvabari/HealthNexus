package org.proj.service;

import org.proj.dto.DoctorAffiliationApplicationRequest;
import org.proj.dto.DoctorAffiliationApplicationResponse;
import org.proj.dto.DoctorAffiliationRequest;
import org.proj.dto.DoctorAffiliationResponse;
import org.proj.dto.ReviewAffiliationRequest;
import org.proj.entity.DoctorAffiliationRequestEntity.RequestStatus;
import org.proj.entity.DoctorHospitalAffiliationEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DoctorHospitalAffiliationService {

    Optional<DoctorHospitalAffiliationEntity> findActiveDutyAffiliation(UUID doctorId);

    Optional<DoctorHospitalAffiliationEntity> findAffiliation(UUID doctorId, UUID hospitalId);

    boolean hasAffiliation(UUID doctorId, UUID hospitalId);

    List<DoctorAffiliationResponse> getDoctorAffiliations(UUID doctorId);

    DoctorAffiliationResponse addOrUpdateAffiliation(UUID doctorId, DoctorAffiliationRequest request);

    void switchActiveHospital(UUID doctorId, UUID targetHospitalId);

    void removeAffiliation(UUID affiliationId);

    // Request & Approval Pipeline
    DoctorAffiliationApplicationResponse submitAffiliationRequest(DoctorAffiliationApplicationRequest request);

    List<DoctorAffiliationApplicationResponse> getMyAffiliationRequests();

    List<DoctorAffiliationApplicationResponse> getPendingRequestsForAdmin(UUID hospitalId, RequestStatus status);

    DoctorAffiliationApplicationResponse reviewAffiliationRequest(UUID requestId, ReviewAffiliationRequest review);
}
