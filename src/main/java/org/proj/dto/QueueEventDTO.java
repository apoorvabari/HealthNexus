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
public class QueueEventDTO {

    public enum QueueEventType {
        QUEUE_CREATED,
        QUEUE_UPDATED,
        PATIENT_CALLED,
        QUEUE_STARTED,
        QUEUE_COMPLETED,
        QUEUE_SKIPPED,
        QUEUE_CANCELLED
    }

    private QueueEventType eventType;
    private QueueResponse queue;
    private UUID hospitalId;
    private UUID doctorId;
    private LocalDateTime timestamp;
}
