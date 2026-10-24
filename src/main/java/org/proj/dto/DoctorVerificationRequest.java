package org.proj.dto;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.proj.entity.DoctorEntity.VerificationStatus;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoctorVerificationRequest {

    @NotNull(message = "Verification status is required")
    private VerificationStatus verificationStatus;

    private Boolean licenseVerified;

    private Boolean degreeVerified;

    private Boolean specializationVerified;

    @jakarta.validation.constraints.Size(max = 500, message = "Verification remarks cannot exceed 500 characters")
    private String verificationRemarks;
    
}
