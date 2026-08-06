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
    private String specialization;

    @NotBlank(message = "Qualification is required")
    private String qualification;

    @NotNull(message = "Experience is required")
    @Min(value = 0, message = "Experience cannot be negative")
    private Integer experience;

    @NotNull(message = "Consultation fee is required")
    @DecimalMin(value = "0.0", message = "Consultation fee cannot be negative")
    private BigDecimal consultationFee;

    @NotBlank(message = "License number is required")
    private String licenseNumber;

    @Builder.Default
    private DoctorStatus status = DoctorStatus.ACTIVE;
}
