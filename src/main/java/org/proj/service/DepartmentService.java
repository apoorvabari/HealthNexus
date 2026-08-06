package org.proj.service;

import org.proj.dto.DepartmentRequest;
import org.proj.dto.DepartmentResponse;

import java.util.List;
import java.util.UUID;

public interface DepartmentService {

    DepartmentResponse createDepartment(DepartmentRequest request);

    DepartmentResponse getDepartmentById(UUID id);

    List<DepartmentResponse> getAllDepartments();

    DepartmentResponse updateDepartment(UUID id, DepartmentRequest request);

    void deleteDepartment(UUID id);
}
