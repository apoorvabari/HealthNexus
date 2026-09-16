package org.proj.service;

import org.proj.dto.HospitalRequest;
import org.proj.dto.HospitalResponse;
import org.proj.dto.PublicHospitalResponse;
import org.proj.entity.HospitalEntity;

import java.util.UUID;

import org.proj.dto.PageResponse;

public interface HospitalService {

    HospitalResponse createHospital(HospitalRequest request);

    HospitalResponse getHospitalById(UUID id);

    PageResponse<HospitalResponse> getAllHospitals(String search, int page, int size);

    java.util.List<PublicHospitalResponse> getPublicHospitals();

    HospitalResponse updateHospital(UUID id, HospitalRequest request);

    void deleteHospital(UUID id);

    HospitalEntity findHospitalById(UUID hospitalId);

    void save(HospitalEntity hospital);

    long count();

    long countByVerificationStatus(HospitalEntity.VerificationStatus verificationStatus);
}
