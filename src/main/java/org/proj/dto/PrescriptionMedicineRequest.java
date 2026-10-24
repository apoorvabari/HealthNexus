package org.proj.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrescriptionMedicineRequest {

    @NotBlank(message = "Medicine name is required")
    @jakarta.validation.constraints.Size(min = 2, max = 150, message = "Medicine name must be between 2 and 150 characters")
    private String medicineName;

    @NotBlank(message = "Dosage is required")
    @jakarta.validation.constraints.Size(min = 1, max = 50, message = "Dosage must be between 1 and 50 characters")
    private String dosage;

    @NotBlank(message = "Frequency is required")
    @jakarta.validation.constraints.Size(min = 1, max = 50, message = "Frequency must be between 1 and 50 characters")
    private String frequency;

    @NotBlank(message = "Duration is required")
    @jakarta.validation.constraints.Size(min = 1, max = 50, message = "Duration must be between 1 and 50 characters")
    private String duration;

    @jakarta.validation.constraints.Size(max = 500, message = "Instructions cannot exceed 500 characters")
    private String instructions;
}
