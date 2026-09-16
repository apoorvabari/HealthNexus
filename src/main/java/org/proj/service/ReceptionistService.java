package org.proj.service;

import org.proj.dto.ReceptionistRequest;
import org.proj.dto.ReceptionistResponse;
import org.proj.entity.ReceptionistEntity;

import java.util.List;
import java.util.UUID;

public interface ReceptionistService {

    ReceptionistResponse createReceptionist(ReceptionistRequest request);

    ReceptionistResponse getReceptionistById(UUID id);

    List<ReceptionistResponse> getAllReceptionists();

    ReceptionistResponse updateReceptionist(UUID id, ReceptionistRequest request);

    void deleteReceptionist(UUID id);

    ReceptionistEntity findReceptionistById(UUID bookedByReceptionistId);

    ReceptionistResponse getReceptionistByAccountId(UUID accountId);

    java.util.Optional<ReceptionistEntity> findReceptionistEntityByAccountId(UUID accountId);

    void callPatient(UUID patientId, String customMessage);

    org.proj.dto.PatientResponse registerWalkInPatient(org.proj.dto.WalkInPatientRegistrationRequest request);
}
