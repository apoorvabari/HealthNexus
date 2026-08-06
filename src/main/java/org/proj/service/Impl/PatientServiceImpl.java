package org.proj.service.Impl;

import org.proj.dto.PatientRequest;
import org.proj.dto.PatientResponse;
import org.proj.entity.PatientEntity;
import org.proj.entity.UserEntity;
import org.proj.entity.HospitalEntity;
import org.proj.mapper.PatientMapper;
import org.proj.repository.PatientRepo;
import org.proj.repository.UserRepo;
import org.proj.repository.HospitalRepo;
import org.proj.service.PatientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class PatientServiceImpl implements PatientService {

    @Autowired
    private PatientRepo patientRepo;

    @Autowired
    private UserRepo userRepo;

    @Autowired
    private HospitalRepo hospitalRepo;

    @Autowired
    private PatientMapper patientMapper;

    @Override
    @Transactional
    public PatientResponse createPatient(PatientRequest request) {
        try {
            if (request.getAccountId() == null) {
                throw new IllegalArgumentException("Account ID is required");
            }

            UserEntity account = userRepo.findById(request.getAccountId())
                    .orElseThrow(() -> new IllegalArgumentException("Account not found"));

            if (account.getRole() == null || !"patient".equals(account.getRole().getRoleName())) {
                throw new IllegalArgumentException("Account is not assigned PATIENT role.");
            }

            if (patientRepo.existsByAccountId(request.getAccountId())) {
                throw new IllegalArgumentException("Patient profile already exists for this account");
            }

            HospitalEntity hospital = hospitalRepo.findById(request.getHospitalId())
                    .orElseThrow(() -> new IllegalArgumentException("Hospital not found"));

            if (patientRepo.existsByPatientCodeAndHospitalId(request.getPatientCode(), request.getHospitalId())) {
                throw new IllegalArgumentException("Patient code already exists in this hospital");
            }

            PatientEntity patient = patientMapper.toEntity(request, account, hospital);
            PatientEntity savedPatient = patientRepo.save(patient);

            PatientResponse response = patientMapper.toResponse(savedPatient);
            response.setMessage("Patient profile created successfully");
            return response;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Unable to create patient profile.", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PatientResponse getPatientById(UUID id) {
        try {
            if (id == null) {
                throw new IllegalArgumentException("Patient Id is required");
            }
            PatientEntity patient = patientRepo.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Patient profile not found"));
            return patientMapper.toResponse(patient);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Unable to fetch patient profile.", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<PatientResponse> getAllPatients() {
        try {
            return patientRepo.findAll().stream()
                    .map(patientMapper::toResponse)
                    .toList();
        } catch (Exception e) {
            throw new RuntimeException("Unable to fetch patient list.", e);
        }
    }

    @Override
    @Transactional
    public PatientResponse updatePatient(UUID id, PatientRequest request) {
        try {
            if (id == null) {
                throw new IllegalArgumentException("Patient Id is required");
            }
            PatientEntity patient = patientRepo.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Patient profile not found"));

            UserEntity account = patient.getAccount();
            if (request.getAccountId() != null && !request.getAccountId().equals(account.getId())) {
                account = userRepo.findById(request.getAccountId())
                        .orElseThrow(() -> new IllegalArgumentException("Account not found"));

                if (account.getRole() == null || !"patient".equals(account.getRole().getRoleName())) {
                    throw new IllegalArgumentException("Account is not assigned PATIENT role.");
                }

                if (patientRepo.existsByAccountIdAndIdNot(request.getAccountId(), id)) {
                    throw new IllegalArgumentException("Patient profile already exists for this account");
                }
            }

            HospitalEntity hospital = patient.getHospital();
            if (request.getHospitalId() != null && !request.getHospitalId().equals(hospital.getId())) {
                hospital = hospitalRepo.findById(request.getHospitalId())
                        .orElseThrow(() -> new IllegalArgumentException("Hospital not found"));
            }

            String targetCode = request.getPatientCode() != null ? request.getPatientCode() : patient.getPatientCode();
            UUID targetHospitalId = hospital.getId();

            if (patientRepo.existsByPatientCodeAndHospitalIdAndIdNot(targetCode, targetHospitalId, id)) {
                throw new IllegalArgumentException("Patient code already exists in this hospital");
            }

            patientMapper.updateEntity(patient, request, account, hospital);
            PatientEntity updatedPatient = patientRepo.save(patient);

            PatientResponse response = patientMapper.toResponse(updatedPatient);
            response.setMessage("Patient profile updated successfully");
            return response;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Unable to update patient profile.", e);
        }
    }

    @Override
    @Transactional
    public void deletePatient(UUID id) {
        try {
            if (id == null) {
                throw new IllegalArgumentException("Patient Id is required");
            }
            PatientEntity patient = patientRepo.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Patient profile not found"));

            patient.setStatus(PatientEntity.PatientStatus.INACTIVE);
            patientRepo.save(patient);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Unable to delete patient profile.", e);
        }
    }
}
