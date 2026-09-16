package org.proj.service;

import org.proj.dto.MedicalHistoryResponse;

import java.util.List;
import java.util.UUID;

public interface MedicalHistoryService {

    List<MedicalHistoryResponse> getPatientMedicalHistory(UUID patientId);

    byte[] generateMedicalHistoryPdf(UUID patientId);
}
