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

    private String verificationRemarks;
    
}
