package org.proj.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.proj.entity.PatientConsentEntity.ConsentStatus;
import org.proj.entity.PatientConsentEntity.ConsentType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsentRequest {

    @NotNull(message = "Consent type is required")
    private ConsentType consentType;

    @NotNull(message = "Consent status is required")
    private ConsentStatus status;
}
