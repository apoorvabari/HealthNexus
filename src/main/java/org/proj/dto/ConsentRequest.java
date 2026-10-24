package org.proj.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.proj.entity.PatientConsentEntity.ConsentStatus;
import org.proj.entity.PatientConsentEntity.ConsentType;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConsentRequest {

    @NotNull(message = "Consent type is required")
    private ConsentType consentType;

    @NotNull(message = "Status is required")
    private ConsentStatus status;

    private UUID doctorId;

    @JsonAlias({"scope", "accessLevel", "access_level"})
    private String scope;

    private String accessLevel;

    private String version;

    public void setConsentType(Object type) {
        if (type == null) return;
        if (type instanceof ConsentType ct) {
            this.consentType = ct;
        } else {
            String s = type.toString().trim().toUpperCase();
            if (s.equals("MEDICAL_RECORDS") || s.equals("MEDICAL_RECORD") || s.equals("ALL")) {
                this.consentType = ConsentType.MEDICAL_RECORD_SHARING;
            } else {
                try {
                    this.consentType = ConsentType.valueOf(s);
                } catch (Exception e) {
                    this.consentType = ConsentType.MEDICAL_RECORD_SHARING;
                }
            }
        }
    }

    public void setStatus(Object stat) {
        if (stat == null) return;
        if (stat instanceof ConsentStatus cs) {
            this.status = cs;
        } else {
            try {
                this.status = ConsentStatus.valueOf(stat.toString().trim().toUpperCase());
            } catch (Exception e) {
                this.status = ConsentStatus.GRANTED;
            }
        }
    }

    public void setDoctorId(Object docId) {
        if (docId == null) {
            this.doctorId = null;
            return;
        }
        if (docId instanceof UUID u) {
            this.doctorId = u;
        } else {
            String str = docId.toString().trim();
            if (str.isEmpty() || str.equalsIgnoreCase("null")) {
                this.doctorId = null;
                return;
            }
            try {
                this.doctorId = UUID.fromString(str);
            } catch (IllegalArgumentException e) {
                this.doctorId = null;
            }
        }
    }

    public void setAccessLevel(String accessLevel) {
        this.accessLevel = accessLevel;
        if (this.scope == null || this.scope.isBlank()) {
            this.scope = accessLevel;
        }
    }
}
