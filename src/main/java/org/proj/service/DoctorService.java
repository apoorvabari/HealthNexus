package org.proj.service;

import org.proj.dto.DoctorRequest;
import org.proj.dto.DoctorResponse;
import org.proj.entity.DoctorEntity;


import java.util.UUID;

import org.proj.dto.PageResponse;

public interface DoctorService {

    DoctorResponse createDoctor(DoctorRequest request);

    DoctorResponse getDoctorById(UUID id);

    PageResponse<DoctorResponse> getAllDoctors(String search, int page, int size, boolean isAdmin);


    DoctorResponse updateDoctor(UUID id, DoctorRequest request);

    void deleteDoctor(UUID id);

    DoctorEntity findDoctorById(UUID doctorId);

    DoctorResponse getDoctorByAccountId(UUID accountId);

    void save(DoctorEntity doctor);

    long count();

    long countByVerificationStatus(DoctorEntity.VerificationStatus verificationStatus);
}
