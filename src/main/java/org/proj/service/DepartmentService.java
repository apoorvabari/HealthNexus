package org.proj.service;

import org.proj.dto.DepartmentRequest;
import org.proj.dto.DepartmentResponse;
import org.proj.entity.DepartmentEntity;

import java.util.UUID;

import org.proj.dto.PageResponse;

public interface DepartmentService {

    DepartmentResponse createDepartment(DepartmentRequest request);

    DepartmentResponse getDepartmentById(UUID id);

    PageResponse<DepartmentResponse> getAllDepartments(String search, int page, int size);

    DepartmentResponse updateDepartment(UUID id, DepartmentRequest request);

    void deleteDepartment(UUID id);

    DepartmentEntity findDepartmentById(UUID departmentId);
}
