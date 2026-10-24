package org.proj.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.proj.entity.DoctorEntity.DoctorStatus;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoctorRequest {

    @NotNull(message = "Account ID is required")
    private UUID accountId;

    @NotNull(message = "Hospital ID is required")
    private UUID hospitalId;

    @NotNull(message = "Department ID is required")
    private UUID departmentId;

    @NotBlank(message = "Specialization is required")
    @Size(min = 2, max = 100, message = "Specialization must be between 2 and 100 characters")
    private String specialization;

    @NotBlank(message = "Qualification is required")
    @Size(min = 2, max = 100, message = "Qualification must be between 2 and 100 characters")
    private String qualification;

    @NotNull(message = "Experience is required")
    @Min(value = 0, message = "Experience cannot be negative")
    @Max(value = 70, message = "Experience cannot exceed 70 years")
    private Integer experience;

    @NotNull(message = "Consultation fee is required")
    @DecimalMin(value = "0.0", message = "Consultation fee cannot be negative")
    @DecimalMax(value = "100000.0", message = "Consultation fee exceeds maximum allowable limit")
    private BigDecimal consultationFee;

    @NotBlank(message = "License number is required")
    @Size(min = 3, max = 50, message = "License number must be between 3 and 50 characters")
    private String licenseNumber;

    @Builder.Default
    private DoctorStatus status = DoctorStatus.ACTIVE;
}
