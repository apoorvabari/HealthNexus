package org.proj.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicalReportRequest {
    
    @NotNull(message = "Patient ID is required")
    private UUID patientId;
    
    @NotBlank(message = "Report name is required")
    private String reportName;
    
    @NotNull(message = "Report type is required")
    private String reportType;
    
    @NotBlank(message = "File data (Base64) is required")
    private String fileData;
}
