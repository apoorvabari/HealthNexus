package org.proj.service;

import org.proj.dto.PatientRequest;
import org.proj.dto.PatientResponse;

import java.util.List;
import java.util.UUID;

public interface PatientService {

    PatientResponse createPatient(PatientRequest request);

    PatientResponse getPatientById(UUID id);

    List<PatientResponse> getAllPatients();

    PatientResponse updatePatient(UUID id, PatientRequest request);

    void deletePatient(UUID id);
}
