package org.proj.service;

import org.proj.dto.HospitalRequest;
import org.proj.dto.HospitalResponse;

import java.util.List;
import java.util.UUID;

public interface HospitalService {

    HospitalResponse createHospital(HospitalRequest request);

    HospitalResponse getHospitalById(UUID id);

    List<HospitalResponse> getAllHospitals();

    HospitalResponse updateHospital(UUID id, HospitalRequest request);

    void deleteHospital(UUID id);
}
