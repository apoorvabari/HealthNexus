package org.proj.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.proj.entity.HospitalEntity.VerificationStatus;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HospitalVerificationRequest {

    @NotNull(message = "Verification status is required")
    private VerificationStatus verificationStatus;

    private Boolean detailsVerified;

    private Boolean locationVerified;

    private String verificationRemarks;
}
