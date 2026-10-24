package org.proj.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicalRecordRequest {

    @NotNull(message = "Patient ID is required")
    private UUID patientId;

    @NotNull(message = "Doctor ID is required")
    private UUID doctorId;

    private UUID appointmentId;

    @NotBlank(message = "Diagnosis is required")
    @jakarta.validation.constraints.Size(min = 2, max = 255, message = "Diagnosis must be between 2 and 255 characters")
    private String diagnosis;

    @jakarta.validation.constraints.Size(max = 2000, message = "Notes cannot exceed 2000 characters")
    private String notes;

    @NotNull(message = "Record Date is required")
    private LocalDate recordDate;
}
