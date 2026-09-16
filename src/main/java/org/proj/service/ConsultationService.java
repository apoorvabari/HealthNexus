package org.proj.service;

import org.proj.dto.ConsultationRequest;
import org.proj.dto.ConsultationResponse;
import java.util.UUID;

public interface ConsultationService {
    ConsultationResponse startConsultation(ConsultationRequest request);
    ConsultationResponse saveVisit(UUID consultationId, String remarks);
    ConsultationResponse completeConsultation(UUID consultationId, String remarks);
    ConsultationResponse getConsultationById(UUID id);
    ConsultationResponse getConsultationByAppointmentId(UUID appointmentId);
}
