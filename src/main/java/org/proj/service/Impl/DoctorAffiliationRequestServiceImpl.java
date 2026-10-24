package org.proj.service.impl;

import lombok.RequiredArgsConstructor;
import org.proj.entity.DoctorAffiliationRequestEntity;
import org.proj.entity.DoctorAffiliationRequestEntity.RequestStatus;
import org.proj.repository.DoctorAffiliationRequestRepo;
import org.proj.service.DoctorAffiliationRequestService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DoctorAffiliationRequestServiceImpl implements DoctorAffiliationRequestService {

    private final DoctorAffiliationRequestRepo affiliationRequestRepo;

    @Override
    @Transactional
    public DoctorAffiliationRequestEntity save(DoctorAffiliationRequestEntity request) {
        try {
            if (request == null) {
                throw new IllegalArgumentException("Doctor affiliation request cannot be null");
            }
            return affiliationRequestRepo.save(request);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            String msg = e.getMessage() != null && !e.getMessage().isBlank()
                    ? e.getMessage()
                    : "Unable to save doctor affiliation request";
            throw new RuntimeException(msg, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DoctorAffiliationRequestEntity> findById(UUID id) {
        try {
            return id == null ? Optional.empty() : affiliationRequestRepo.findById(id);
        } catch (Exception e) {
            String msg = e.getMessage() != null && !e.getMessage().isBlank()
                    ? e.getMessage()
                    : "Unable to find doctor affiliation request";
            throw new RuntimeException(msg, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByDoctorIdAndTargetHospitalIdAndStatus(UUID doctorId, UUID targetHospitalId, RequestStatus status) {
        try {
            return (doctorId == null || targetHospitalId == null || status == null)
                    ? false
                    : affiliationRequestRepo.existsByDoctorIdAndTargetHospitalIdAndStatus(doctorId, targetHospitalId, status);
        } catch (Exception e) {
            String msg = e.getMessage() != null && !e.getMessage().isBlank()
                    ? e.getMessage()
                    : "Unable to verify affiliation request existence";
            throw new RuntimeException(msg, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<DoctorAffiliationRequestEntity> findByDoctorIdOrderByCreatedAtDesc(UUID doctorId) {
        try {
            return doctorId == null ? List.of() : affiliationRequestRepo.findByDoctorIdOrderByCreatedAtDesc(doctorId);
        } catch (Exception e) {
            String msg = e.getMessage() != null && !e.getMessage().isBlank()
                    ? e.getMessage()
                    : "Unable to find doctor affiliation requests by doctor ID";
            throw new RuntimeException(msg, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<DoctorAffiliationRequestEntity> findByTargetHospitalIdOrderByCreatedAtDesc(UUID hospitalId) {
        try {
            return hospitalId == null ? List.of() : affiliationRequestRepo.findByTargetHospitalIdOrderByCreatedAtDesc(hospitalId);
        } catch (Exception e) {
            String msg = e.getMessage() != null && !e.getMessage().isBlank()
                    ? e.getMessage()
                    : "Unable to find affiliation requests by target hospital ID";
            throw new RuntimeException(msg, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<DoctorAffiliationRequestEntity> findByTargetHospitalIdAndStatusOrderByCreatedAtDesc(UUID hospitalId, RequestStatus status) {
        try {
            return (hospitalId == null || status == null)
                    ? List.of()
                    : affiliationRequestRepo.findByTargetHospitalIdAndStatusOrderByCreatedAtDesc(hospitalId, status);
        } catch (Exception e) {
            String msg = e.getMessage() != null && !e.getMessage().isBlank()
                    ? e.getMessage()
                    : "Unable to find affiliation requests by target hospital and status";
            throw new RuntimeException(msg, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<DoctorAffiliationRequestEntity> findByTargetHospitalIds(List<UUID> hospitalIds) {
        try {
            return (hospitalIds == null || hospitalIds.isEmpty())
                    ? List.of()
                    : affiliationRequestRepo.findByTargetHospitalIds(hospitalIds);
        } catch (Exception e) {
            String msg = e.getMessage() != null && !e.getMessage().isBlank()
                    ? e.getMessage()
                    : "Unable to find affiliation requests by target hospitals";
            throw new RuntimeException(msg, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<DoctorAffiliationRequestEntity> findByTargetHospitalIdsAndStatus(List<UUID> hospitalIds, RequestStatus status) {
        try {
            return (hospitalIds == null || hospitalIds.isEmpty() || status == null)
                    ? List.of()
                    : affiliationRequestRepo.findByTargetHospitalIdsAndStatus(hospitalIds, status);
        } catch (Exception e) {
            String msg = e.getMessage() != null && !e.getMessage().isBlank()
                    ? e.getMessage()
                    : "Unable to find affiliation requests by hospitals and status";
            throw new RuntimeException(msg, e);
        }
    }

}
