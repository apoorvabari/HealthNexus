package org.proj.service;

import org.proj.dto.MedicalRecordRequest;
import org.proj.dto.MedicalRecordResponse;
import org.proj.entity.MedicalRecordEntity;

import java.util.List;
import java.util.UUID;

public interface MedicalRecordService {
    MedicalRecordResponse createMedicalRecord(MedicalRecordRequest request);
    MedicalRecordResponse getMedicalRecordById(UUID id);
    MedicalRecordEntity findMedicalRecordEntityById(UUID id);
    List<MedicalRecordResponse> getPatientMedicalRecords(UUID patientId);
    List<MedicalRecordResponse> getDoctorMedicalRecords(UUID doctorId);
    MedicalRecordResponse getMedicalRecordByAppointment(UUID appointmentId);
}
