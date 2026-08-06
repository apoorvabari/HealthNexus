package org.proj.service.Impl;

import org.proj.dto.HospitalRequest;
import org.proj.dto.HospitalResponse;
import org.proj.entity.HospitalEntity;
import org.proj.mapper.HospitalMapper;
import org.proj.repository.HospitalRepo;
import org.proj.service.HospitalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class HospitalServiceImpl implements HospitalService {

    @Autowired
    private HospitalRepo hospitalRepo;

    @Autowired
    private HospitalMapper hospitalMapper;

    @Override
    @Transactional
    public HospitalResponse createHospital(HospitalRequest request) {
        try {
            if (hospitalRepo.existsByEmail(request.getEmail().trim().toLowerCase())) {
                throw new IllegalArgumentException("Hospital with this email already exists");
            }
            if (hospitalRepo.existsByRegistrationNumber(request.getRegistrationNumber())) {
                throw new IllegalArgumentException("Hospital with this registration number already exists");
            }

            HospitalEntity hospital = hospitalMapper.toEntity(request);
            hospital.setHospitalCode(generateHospitalCode());

            HospitalEntity savedHospital = hospitalRepo.save(hospital);

            HospitalResponse response = hospitalMapper.toResponse(savedHospital);
            response.setMessage("Hospital created successfully");
            return response;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Unable to create hospital.", e);
        }
    }

    @Override
    public HospitalResponse getHospitalById(UUID id) {
        try {
            if (id == null) {
                throw new IllegalArgumentException("Hospital Id is required");
            }
            HospitalEntity hospital = hospitalRepo.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Hospital not found"));
            return hospitalMapper.toResponse(hospital);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Unable to fetch hospital.", e);
        }
    }

    @Override
    public List<HospitalResponse> getAllHospitals() {
        try {
            return hospitalRepo.findAll().stream()
                    .map(hospitalMapper::toResponse)
                    .toList();
        } catch (Exception e) {
            throw new RuntimeException("Unable to fetch hospital list.", e);
        }
    }

    @Override
    @Transactional
    public HospitalResponse updateHospital(UUID id, HospitalRequest request) {
        try {
            if (id == null) {
                throw new IllegalArgumentException("Hospital Id is required");
            }
            HospitalEntity hospital = hospitalRepo.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Hospital not found"));

            if (request.getEmail() != null && !request.getEmail().trim().toLowerCase().equals(hospital.getEmail())) {
                if (hospitalRepo.existsByEmail(request.getEmail().trim().toLowerCase())) {
                    throw new IllegalArgumentException("Hospital with this email already exists");
                }
            }

            if (request.getRegistrationNumber() != null
                    && !request.getRegistrationNumber().equals(hospital.getRegistrationNumber())) {
                if (hospitalRepo.existsByRegistrationNumber(request.getRegistrationNumber())) {
                    throw new IllegalArgumentException("Hospital with this registration number already exists");
                }
            }

            hospitalMapper.updateEntity(hospital, request);
            HospitalEntity updatedHospital = hospitalRepo.save(hospital);

            HospitalResponse response = hospitalMapper.toResponse(updatedHospital);
            response.setMessage("Hospital updated successfully");
            return response;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Unable to update hospital.", e);
        }
    }

    @Override
    @Transactional
    public void deleteHospital(UUID id) {
        try {
            if (id == null) {
                throw new IllegalArgumentException("Hospital Id is required");
            }
            HospitalEntity hospital = hospitalRepo.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Hospital not found"));

            hospital.setStatus(HospitalEntity.HospitalStatus.INACTIVE);
            hospitalRepo.save(hospital);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Unable to delete hospital.", e);
        }
    }

    private synchronized String generateHospitalCode() {
        String maxCode = hospitalRepo.findMaxHospitalCode();
        if (maxCode == null || maxCode.isBlank()) {
            return "HN001";
        }

        try {
            if (maxCode.startsWith("HN") && maxCode.length() > 2) {
                String numberPart = maxCode.substring(2);
                int number = Integer.parseInt(numberPart);
                return String.format("HN%03d", number + 1);
            }
        } catch (NumberFormatException e) {
        }

        long count = hospitalRepo.count();
        return String.format("HN%03d", count + 1);
    }
}
