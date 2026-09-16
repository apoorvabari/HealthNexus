package org.proj.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.proj.entity.DoctorEntity.DoctorStatus;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicDoctorResponse {
    private UUID id;

    private UUID hospitalId;
    private String hospitalName;

    private UUID departmentId;
    private String departmentName;

    private String doctorName;
    private String specialization;
    private String qualification;
    private Integer experience;
    private BigDecimal consultationFee;
    
    private DoctorStatus status;
}
