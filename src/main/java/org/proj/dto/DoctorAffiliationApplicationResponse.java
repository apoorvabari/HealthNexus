package org.proj.dto;

import lombok.*;
import org.proj.entity.DoctorAffiliationRequestEntity.RequestStatus;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoctorAffiliationApplicationResponse {

    private UUID id;
    private UUID doctorId;
    private String doctorName;
    private String doctorSpecialization;
    private UUID hospitalId;
    private String hospitalName;
    private UUID departmentId;
    private String departmentName;
    private RequestStatus status;
    private String reason;
    private String adminNotes;
    private LocalDateTime createdAt;
    private LocalDateTime reviewedAt;
}
