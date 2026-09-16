package org.proj.service;

import org.proj.dto.MedicalReportRequest;
import org.proj.dto.MedicalReportResponse;

import java.util.List;
import java.util.UUID;

public interface MedicalReportService {
    MedicalReportResponse uploadReport(MedicalReportRequest request);
    MedicalReportResponse getReportById(UUID id);
    List<MedicalReportResponse> getReportsByPatientId(UUID patientId);
    void deleteReport(UUID id);
}
