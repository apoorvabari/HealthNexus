package org.proj.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.proj.entity.QueueEntity.QueueStatus;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueueResponse {
    private UUID id;
    private Integer queueNumber;
    private String tokenNumber;
    private UUID hospitalId;
    private String hospitalName;
    private UUID departmentId;
    private String departmentName;
    private UUID doctorId;
    private String doctorName;
    private UUID appointmentId;
    private String appointmentNumber;
    private QueueStatus queueStatus;
    private LocalDateTime checkedInTime;
    private LocalDateTime consultationStart;
    private LocalDateTime consultationEnd;
    private String message;
}
