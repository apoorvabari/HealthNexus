package org.proj.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.proj.entity.DoctorHospitalAffiliationEntity.AffiliationStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoctorAffiliationResponse {

    private UUID id;
    private UUID doctorId;
    private String doctorName;
    private UUID hospitalId;
    private String hospitalName;
    private String hospitalAddress;
    private String hospitalCity;
    private UUID departmentId;
    private String departmentName;
    private BigDecimal consultationFee;
    private boolean activeDuty;
    private AffiliationStatus status;
    private LocalDateTime createdAt;
}
