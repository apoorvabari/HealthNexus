package org.proj.service;

import org.proj.dto.ConsentRequest;
import org.proj.dto.ConsentResponse;
import org.proj.entity.PatientConsentEntity.ConsentType;

import org.proj.dto.ConsentRequestDto;
import java.util.List;
import java.util.UUID;

public interface PatientConsentService {

    ConsentResponse updateConsent(ConsentRequest request);

    List<ConsentResponse> getMyConsents();

    ConsentResponse getConsentByType(ConsentType type);

    ConsentResponse revokeConsent(ConsentType type);

    ConsentResponse revokeConsentForDoctor(ConsentType type, UUID doctorId);

    ConsentResponse grantDoctorConsent(ConsentRequestDto request);

    ConsentResponse toggleStandardConsent(ConsentRequest request);

    ConsentResponse revokeDoctorConsent(Object doctorId);

    void verifyConsent(UUID patientId, ConsentType type);

    void verifyConsentForDoctor(UUID patientId, UUID doctorId, ConsentType type, String requiredScope);
}
