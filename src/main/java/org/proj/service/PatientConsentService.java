package org.proj.service;

import org.proj.dto.ConsentRequest;
import org.proj.dto.ConsentResponse;
import org.proj.entity.PatientConsentEntity.ConsentType;

import java.util.List;
import java.util.UUID;

public interface PatientConsentService {

    ConsentResponse updateConsent(ConsentRequest request);

    List<ConsentResponse> getMyConsents();

    ConsentResponse getConsentByType(ConsentType type);

    ConsentResponse revokeConsent(ConsentType type);

    void verifyConsent(UUID patientId, ConsentType type);
}
