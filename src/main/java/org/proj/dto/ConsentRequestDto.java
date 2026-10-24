package org.proj.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConsentRequestDto {

    @NotNull(message = "Doctor ID is required")
    @JsonAlias({"doctorId", "doctor_id"})
    private Object doctorId;

    @JsonAlias({"consentType", "consent_type"})
    private String consentType; // e.g., MEDICAL_RECORDS, MEDICAL_RECORD_SHARING

    @JsonAlias({"accessLevel", "access_level", "scope"})
    private String accessLevel; // ALL_RECORDS, APPOINTMENT_ONLY

    private String scope;

    private String status;      // GRANTED, REVOKED

    private String version;
}
