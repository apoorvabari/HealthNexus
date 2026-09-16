package org.proj.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicalHistoryResponse {

    private Integer visitNumber;

    private UUID visitId;

    private UUID appointmentId;

    private String appointmentNumber;

    private LocalDate visitDate;

    private UUID doctorId;

    private String doctorName;

    private String doctorSpecialization;

    private ConsultationResponse consultation;

    private MedicalRecordResponse medicalRecord;

    private List<PrescriptionResponse> prescriptions;
}
