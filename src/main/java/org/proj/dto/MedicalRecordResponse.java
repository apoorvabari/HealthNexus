package org.proj.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicalRecordResponse {

    private UUID id;

    private UUID patientId;
    private String patientName;
    private String patientCode;

    private UUID doctorId;
    private String doctorName;
    private String doctorSpecialization;

    private UUID appointmentId;
    private String appointmentNumber;

    private String diagnosis;
    private String notes;
    private LocalDate recordDate;
    private LocalDateTime createdAt;
    
    private String message;
}
