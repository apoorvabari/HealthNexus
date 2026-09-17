package org.proj.service;

import org.proj.dto.QueueRequest;
import org.proj.dto.QueueResponse;

import java.util.List;
import java.util.UUID;

public interface QueueService {

    QueueResponse checkIn(QueueRequest request);

    List<QueueResponse> getTodayQueue();

    List<QueueResponse> getTodayQueueByDoctor(UUID doctorId);

    QueueResponse callNext(UUID doctorId);

    QueueResponse startConsultation(UUID queueId);

    QueueResponse completeConsultation(UUID queueId);

    QueueResponse skipQueue(UUID queueId);

    QueueResponse recallSkippedPatient(UUID queueId);
}
