package org.proj.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.proj.entity.PatientConsentEntity.ConsentStatus;
import org.proj.entity.PatientConsentEntity.ConsentType;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsentResponse {

    private UUID id;
    private ConsentType consentType;
    private ConsentStatus status;
    private LocalDateTime consentedAt;
    private LocalDateTime revokedAt;
}
