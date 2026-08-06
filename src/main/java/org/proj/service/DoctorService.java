package org.proj.service;

import org.proj.dto.DoctorRequest;
import org.proj.dto.DoctorResponse;

import java.util.List;
import java.util.UUID;

public interface DoctorService {

    DoctorResponse createDoctor(DoctorRequest request);

    DoctorResponse getDoctorById(UUID id);

    List<DoctorResponse> getAllDoctors();

    DoctorResponse updateDoctor(UUID id, DoctorRequest request);

    void deleteDoctor(UUID id);
}
