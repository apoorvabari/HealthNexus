package org.proj.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.proj.entity.HospitalEntity.HospitalType;
import org.proj.entity.HospitalEntity.HospitalStatus;
import org.proj.entity.HospitalEntity.VerificationStatus;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.PrePersist;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HospitalResponse {
    private UUID id;
    private String hospitalCode;
    private String hospitalName;
    private String email;
    private String phoneNumber;
    private String address;
    private String city;
    private String state;
    private String postalCode;
    private HospitalType hospitalType;
    private String registrationNumber;
    private HospitalStatus status;
    private VerificationStatus verificationStatus;

    private Boolean detailsVerified;

    private Boolean locationVerified;

    private String verificationRemarks;

    private UUID verifiedBy;

    private LocalDateTime verifiedAt;

    private String message;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {

        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();

        if (this.status == null) {
            this.status = HospitalStatus.ACTIVE;
        }

        if (this.verificationStatus == null) {
            this.verificationStatus = VerificationStatus.PENDING;
        }

        if (this.detailsVerified == null) {
            this.detailsVerified = false;
        }

        if (this.locationVerified == null) {
            this.locationVerified = false;
        }

        if (this.verificationRemarks == null) {
            this.verificationRemarks = "Not yet Verified";
        }
    }
}
