package org.proj.service.Impl;

import org.proj.dto.ReceptionistRequest;
import org.proj.dto.ReceptionistResponse;
import org.proj.entity.ReceptionistEntity;
import org.proj.entity.UserEntity;
import org.proj.entity.HospitalEntity;
import org.proj.entity.DepartmentEntity;
import org.proj.mapper.ReceptionistMapper;
import org.proj.repository.ReceptionistRepo;
import org.proj.repository.UserRepo;
import org.proj.repository.HospitalRepo;
import org.proj.repository.DepartmentRepo;
import org.proj.service.ReceptionistService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ReceptionistServiceImpl implements ReceptionistService {

    @Autowired
    private ReceptionistRepo receptionistRepo;

    @Autowired
    private UserRepo userRepo;

    @Autowired
    private HospitalRepo hospitalRepo;

    @Autowired
    private DepartmentRepo departmentRepo;

    @Autowired
    private ReceptionistMapper receptionistMapper;

    @Override
    @Transactional
    public ReceptionistResponse createReceptionist(ReceptionistRequest request) {
        try {
            if (request.getAccountId() == null) {
                throw new IllegalArgumentException("Account ID is required");
            }

            UserEntity account = userRepo.findById(request.getAccountId())
                    .orElseThrow(() -> new IllegalArgumentException("Account not found"));

            if (account.getRole() == null || !"receptionist".equals(account.getRole().getRoleName())) {
                throw new IllegalArgumentException("Account is not assigned RECEPTIONIST role.");
            }

            if (receptionistRepo.existsByAccountId(request.getAccountId())) {
                throw new IllegalArgumentException("Receptionist profile already exists for this account");
            }

            HospitalEntity hospital = hospitalRepo.findById(request.getHospitalId())
                    .orElseThrow(() -> new IllegalArgumentException("Hospital not found"));

            DepartmentEntity department = departmentRepo.findById(request.getDepartmentId())
                    .orElseThrow(() -> new IllegalArgumentException("Department not found"));

            if (receptionistRepo.existsByEmployeeCodeAndHospitalId(request.getEmployeeCode(), request.getHospitalId())) {
                throw new IllegalArgumentException("Employee code already exists in this hospital");
            }

            ReceptionistEntity receptionist = receptionistMapper.toEntity(request, account, hospital, department);
            ReceptionistEntity savedReceptionist = receptionistRepo.save(receptionist);

            ReceptionistResponse response = receptionistMapper.toResponse(savedReceptionist);
            response.setMessage("Receptionist profile created successfully");
            return response;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Unable to create receptionist profile.", e);
        }
    }

    @Override
    public ReceptionistResponse getReceptionistById(UUID id) {
        try {
            if (id == null) {
                throw new IllegalArgumentException("Receptionist Id is required");
            }
            ReceptionistEntity receptionist = receptionistRepo.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Receptionist profile not found"));
            return receptionistMapper.toResponse(receptionist);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Unable to fetch receptionist profile.", e);
        }
    }

    @Override
    public List<ReceptionistResponse> getAllReceptionists() {
        try {
            return receptionistRepo.findAll().stream()
                    .map(receptionistMapper::toResponse)
                    .toList();
        } catch (Exception e) {
            throw new RuntimeException("Unable to fetch receptionist list.", e);
        }
    }

    @Override
    @Transactional
    public ReceptionistResponse updateReceptionist(UUID id, ReceptionistRequest request) {
        try {
            if (id == null) {
                throw new IllegalArgumentException("Receptionist Id is required");
            }
            ReceptionistEntity receptionist = receptionistRepo.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Receptionist profile not found"));

            UserEntity account = receptionist.getAccount();
            if (request.getAccountId() != null && !request.getAccountId().equals(account.getId())) {
                account = userRepo.findById(request.getAccountId())
                        .orElseThrow(() -> new IllegalArgumentException("Account not found"));

                if (account.getRole() == null || !"receptionist".equals(account.getRole().getRoleName())) {
                    throw new IllegalArgumentException("Account is not assigned RECEPTIONIST role.");
                }

                if (receptionistRepo.existsByAccountIdAndIdNot(request.getAccountId(), id)) {
                    throw new IllegalArgumentException("Receptionist profile already exists for this account");
                }
            }

            HospitalEntity hospital = receptionist.getHospital();
            if (request.getHospitalId() != null && !request.getHospitalId().equals(hospital.getId())) {
                hospital = hospitalRepo.findById(request.getHospitalId())
                        .orElseThrow(() -> new IllegalArgumentException("Hospital not found"));
            }

            DepartmentEntity department = receptionist.getDepartment();
            if (request.getDepartmentId() != null && !request.getDepartmentId().equals(department.getId())) {
                department = departmentRepo.findById(request.getDepartmentId())
                        .orElseThrow(() -> new IllegalArgumentException("Department not found"));
            }

            String targetCode = request.getEmployeeCode() != null ? request.getEmployeeCode()
                    : receptionist.getEmployeeCode();
            UUID targetHospitalId = hospital.getId();

            if (receptionistRepo.existsByEmployeeCodeAndHospitalIdAndIdNot(targetCode, targetHospitalId, id)) {
                throw new IllegalArgumentException("Employee code already exists in this hospital");
            }

            receptionistMapper.updateEntity(receptionist, request, account, hospital, department);
            ReceptionistEntity updatedReceptionist = receptionistRepo.save(receptionist);

            ReceptionistResponse response = receptionistMapper.toResponse(updatedReceptionist);
            response.setMessage("Receptionist profile updated successfully");
            return response;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Unable to update receptionist profile.", e);
        }
    }

    @Override
    @Transactional
    public void deleteReceptionist(UUID id) {
        try {
            if (id == null) {
                throw new IllegalArgumentException("Receptionist Id is required");
            }
            ReceptionistEntity receptionist = receptionistRepo.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Receptionist profile not found"));

            receptionist.setStatus(ReceptionistEntity.ReceptionistStatus.INACTIVE);
            receptionistRepo.save(receptionist);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Unable to delete receptionist profile.", e);
        }
    }
}
