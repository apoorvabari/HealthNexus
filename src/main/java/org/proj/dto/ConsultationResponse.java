package org.proj.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsultationResponse {
    
    private UUID id;
    private UUID appointmentId;
    private UUID doctorId;
    private String doctorName;
    private UUID patientId;
    private String patientName;
    private String status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String remarks;
    private boolean visitSaved;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
