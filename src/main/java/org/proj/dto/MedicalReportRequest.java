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
    @jakarta.validation.constraints.Size(min = 2, max = 150, message = "Report name must be between 2 and 150 characters")
    private String reportName;
    
    @NotNull(message = "Report type is required")
    @jakarta.validation.constraints.Size(min = 2, max = 50, message = "Report type must be between 2 and 50 characters")
    private String reportType;
    
    @NotBlank(message = "File data (Base64) is required")
    private String fileData;
}
