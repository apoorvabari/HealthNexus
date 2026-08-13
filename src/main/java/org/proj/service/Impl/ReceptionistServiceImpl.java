package org.proj.service.Impl;

import org.proj.dto.ReceptionistRequest;
import org.proj.dto.ReceptionistResponse;
import org.proj.entity.ReceptionistEntity;
import org.proj.entity.UserEntity;
import org.proj.entity.HospitalEntity;
import org.proj.entity.DepartmentEntity;
import org.proj.mapper.ReceptionistMapper;
import org.proj.repository.ReceptionistRepo;
import org.proj.service.DepartmentService;
import org.proj.service.HospitalService;
import org.proj.service.ReceptionistService;
import org.proj.service.UserService;
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
    private UserService userService;

    @Autowired
    private HospitalService hospitalService;

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private ReceptionistMapper receptionistMapper;

    @Override
    @Transactional
    public ReceptionistResponse createReceptionist(ReceptionistRequest request) {
        try {
            if (request.getAccountId() == null) {
                throw new IllegalArgumentException("Account ID is required");
            }

            UserEntity account = userService.findUserById(request.getAccountId());

            if (account.getRole() == null || !"RECEPTIONIST".equalsIgnoreCase(account.getRole().getRoleName())) {
                throw new IllegalArgumentException("Account is not assigned RECEPTIONIST role.");
            }

            if (receptionistRepo.existsByAccountId(request.getAccountId())) {
                throw new IllegalArgumentException("Receptionist profile already exists for this account");
            }

            HospitalEntity hospital = hospitalService.findHospitalById(request.getHospitalId());

            DepartmentEntity department = departmentService.findDepartmentById(request.getDepartmentId());

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
                account = userService.findUserById(request.getAccountId());

                if (account.getRole() == null || !"RECEPTIONIST".equalsIgnoreCase(account.getRole().getRoleName())) {
                    throw new IllegalArgumentException("Account is not assigned RECEPTIONIST role.");
                }

                if (receptionistRepo.existsByAccountIdAndIdNot(request.getAccountId(), id)) {
                    throw new IllegalArgumentException("Receptionist profile already exists for this account");
                }
            }

            HospitalEntity hospital = receptionist.getHospital();
            if (request.getHospitalId() != null && !request.getHospitalId().equals(hospital.getId())) {
                hospital = hospitalService.findHospitalById(request.getHospitalId());
            }

            DepartmentEntity department = receptionist.getDepartment();
            if (request.getDepartmentId() != null && !request.getDepartmentId().equals(department.getId())) {
                department = departmentService.findDepartmentById(request.getDepartmentId());
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

    @Override
    public ReceptionistEntity findReceptionistById(UUID bookedByReceptionistId) {
        return receptionistRepo.findById(bookedByReceptionistId)
                .orElseThrow(() -> new IllegalArgumentException("Receptionist not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public ReceptionistResponse getReceptionistByAccountId(UUID accountId) {
        ReceptionistEntity receptionist = receptionistRepo.findByAccountId(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Receptionist profile not found for this account"));
        return receptionistMapper.toResponse(receptionist);
    }

}
