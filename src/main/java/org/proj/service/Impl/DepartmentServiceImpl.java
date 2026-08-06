package org.proj.service.Impl;

import org.proj.dto.DepartmentRequest;
import org.proj.dto.DepartmentResponse;
import org.proj.entity.DepartmentEntity;
import org.proj.entity.HospitalEntity;
import org.proj.mapper.DepartmentMapper;
import org.proj.repository.DepartmentRepo;
import org.proj.repository.HospitalRepo;
import org.proj.service.DepartmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class DepartmentServiceImpl implements DepartmentService {

    @Autowired
    private DepartmentRepo departmentRepo;

    @Autowired
    private HospitalRepo hospitalRepo;

    @Autowired
    private DepartmentMapper departmentMapper;

    @Override
    @Transactional
    public DepartmentResponse createDepartment(DepartmentRequest request) {
        try {
            if (request.getHospitalId() == null) {
                throw new IllegalArgumentException("Hospital ID is required");
            }

            HospitalEntity hospital = hospitalRepo.findById(request.getHospitalId())
                    .orElseThrow(() -> new IllegalArgumentException("Hospital not found"));

            if (departmentRepo.existsByDepartmentCodeAndHospitalId(request.getDepartmentCode(), request.getHospitalId())) {
                throw new IllegalArgumentException("Department code already exists in this hospital");
            }

            DepartmentEntity department = departmentMapper.toEntity(request, hospital);
            DepartmentEntity savedDepartment = departmentRepo.save(department);

            DepartmentResponse response = departmentMapper.toResponse(savedDepartment);
            response.setMessage("Department created successfully");
            return response;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Unable to create department.", e);
        }
    }

    @Override
    public DepartmentResponse getDepartmentById(UUID id) {
        try {
            if (id == null) {
                throw new IllegalArgumentException("Department Id is required");
            }
            DepartmentEntity department = departmentRepo.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Department not found"));
            return departmentMapper.toResponse(department);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Unable to fetch department.", e);
        }
    }

    @Override
    public List<DepartmentResponse> getAllDepartments() {
        try {
            return departmentRepo.findAll().stream()
                    .map(departmentMapper::toResponse)
                    .toList();
        } catch (Exception e) {
            throw new RuntimeException("Unable to fetch department list.", e);
        }
    }

    @Override
    @Transactional
    public DepartmentResponse updateDepartment(UUID id, DepartmentRequest request) {
        try {
            if (id == null) {
                throw new IllegalArgumentException("Department Id is required");
            }
            DepartmentEntity department = departmentRepo.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Department not found"));

            HospitalEntity hospital = department.getHospital();
            if (request.getHospitalId() != null && !request.getHospitalId().equals(hospital.getId())) {
                hospital = hospitalRepo.findById(request.getHospitalId())
                        .orElseThrow(() -> new IllegalArgumentException("Hospital not found"));
            }

            String targetCode = request.getDepartmentCode() != null ? request.getDepartmentCode()
                    : department.getDepartmentCode();
            UUID targetHospitalId = hospital.getId();

            if (departmentRepo.existsByDepartmentCodeAndHospitalIdAndIdNot(targetCode, targetHospitalId, id)) {
                throw new IllegalArgumentException("Department code already exists in this hospital");
            }

            departmentMapper.updateEntity(department, request, hospital);
            DepartmentEntity updatedDepartment = departmentRepo.save(department);

            DepartmentResponse response = departmentMapper.toResponse(updatedDepartment);
            response.setMessage("Department updated successfully");
            return response;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Unable to update department.", e);
        }
    }

    @Override
    @Transactional
    public void deleteDepartment(UUID id) {
        try {
            if (id == null) {
                throw new IllegalArgumentException("Department Id is required");
            }
            DepartmentEntity department = departmentRepo.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Department not found"));

            department.setStatus(DepartmentEntity.DepartmentStatus.INACTIVE);
            departmentRepo.save(department);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Unable to delete department.", e);
        }
    }
}
