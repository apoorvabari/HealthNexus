package org.proj.service;

import org.proj.dto.ReceptionistRequest;
import org.proj.dto.ReceptionistResponse;

import java.util.List;
import java.util.UUID;

public interface ReceptionistService {

    ReceptionistResponse createReceptionist(ReceptionistRequest request);

    ReceptionistResponse getReceptionistById(UUID id);

    List<ReceptionistResponse> getAllReceptionists();

    ReceptionistResponse updateReceptionist(UUID id, ReceptionistRequest request);

    void deleteReceptionist(UUID id);
}
