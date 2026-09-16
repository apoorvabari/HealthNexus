package org.proj.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrescriptionRequest {

    @NotNull(message = "Appointment ID is required")
    private UUID appointmentId;

    private UUID medicalRecordId;

    private String instructions;

    @NotEmpty(message = "At least one medicine is required")
    @Valid
    private List<PrescriptionMedicineRequest> medicines;
}
