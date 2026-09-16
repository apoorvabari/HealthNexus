package org.proj.service;

import org.proj.dto.PrescriptionRequest;
import org.proj.dto.PrescriptionResponse;

import java.util.List;
import java.util.UUID;

public interface PrescriptionService {
    PrescriptionResponse createPrescription(PrescriptionRequest request);
    PrescriptionResponse getPrescriptionById(UUID id);
    List<PrescriptionResponse> getPatientPrescriptions(UUID patientId);
    List<PrescriptionResponse> getDoctorPrescriptions(UUID doctorId);
}
