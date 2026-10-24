package org.proj.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.proj.entity.DoctorHospitalAffiliationEntity.AffiliationStatus;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoctorAffiliationRequest {

    @NotNull(message = "Hospital ID is required")
    private UUID hospitalId;

    @NotNull(message = "Department ID is required")
    private UUID departmentId;

    @jakarta.validation.constraints.DecimalMin(value = "0.0", inclusive = true, message = "Consultation fee cannot be negative")
    @jakarta.validation.constraints.DecimalMax(value = "100000.0", message = "Consultation fee exceeds maximum allowable limit")
    private BigDecimal consultationFee;

    private Boolean activeDuty;

    private AffiliationStatus status;
}
