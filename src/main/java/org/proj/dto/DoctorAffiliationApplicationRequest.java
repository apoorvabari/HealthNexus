package org.proj.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoctorAffiliationApplicationRequest {

    @NotNull(message = "Hospital ID is required")
    private UUID hospitalId;

    @NotNull(message = "Department ID is required")
    private UUID departmentId;

    @Size(max = 500, message = "Reason cannot exceed 500 characters")
    private String reason;
}
