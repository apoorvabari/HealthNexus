package org.proj.service.Impl;

import org.proj.dto.DepartmentRequest;
import org.proj.dto.DepartmentResponse;
import org.proj.entity.DepartmentEntity;
import org.proj.entity.HospitalEntity;
import org.proj.mapper.DepartmentMapper;
import org.proj.repository.DepartmentRepo;
import org.proj.service.DepartmentService;
import org.proj.service.HospitalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import org.proj.dto.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

@Service
public class DepartmentServiceImpl implements DepartmentService {

    @Autowired
    private DepartmentRepo departmentRepo;

    @Autowired
    private HospitalService hospitalService;

    @Autowired
    private DepartmentMapper departmentMapper;

    @Override
    @Transactional
    public DepartmentResponse createDepartment(DepartmentRequest request) {
        try {
            if (request.getHospitalId() == null) {
                throw new IllegalArgumentException("Hospital ID is required");
            }

            HospitalEntity hospital = hospitalService.findHospitalById(request.getHospitalId());

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
    @Transactional(readOnly = true)
    public PageResponse<DepartmentResponse> getAllDepartments(String search, int page, int size) {
        try {
            Page<DepartmentEntity> deptPage = departmentRepo.searchDepartments(search, PageRequest.of(page, size));
            List<DepartmentResponse> content = deptPage.getContent().stream()
                    .map(departmentMapper::toResponse)
                    .toList();
            return new PageResponse<>(content, deptPage.getNumber(), deptPage.getSize(), deptPage.getTotalElements(), deptPage.getTotalPages(), deptPage.isLast());
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
                hospital = hospitalService.findHospitalById(request.getHospitalId());
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

    @Override
    public DepartmentEntity findDepartmentById(UUID departmentId) {
        return departmentRepo.findById(departmentId)
                .orElseThrow(() -> new IllegalArgumentException("Department not found"));
    }


}
