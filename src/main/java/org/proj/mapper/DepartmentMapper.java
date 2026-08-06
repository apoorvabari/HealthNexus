package org.proj.mapper;

import org.proj.dto.DepartmentRequest;
import org.proj.dto.DepartmentResponse;
import org.proj.entity.DepartmentEntity;
import org.proj.entity.HospitalEntity;
import org.springframework.stereotype.Component;

@Component
public class DepartmentMapper {

    public DepartmentEntity toEntity(DepartmentRequest request, HospitalEntity hospital) {
        if (request == null) {
            return null;
        }
        return DepartmentEntity.builder()
                .departmentCode(request.getDepartmentCode())
                .departmentName(request.getDepartmentName())
                .description(request.getDescription())
                .status(request.getStatus() != null ? request.getStatus() : DepartmentEntity.DepartmentStatus.ACTIVE)
                .hospital(hospital)
                .build();
    }

    public void updateEntity(DepartmentEntity department, DepartmentRequest request, HospitalEntity hospital) {
        if (department == null || request == null) {
            return;
        }
        department.setDepartmentCode(request.getDepartmentCode() != null ? request.getDepartmentCode() : department.getDepartmentCode());
        department.setDepartmentName(request.getDepartmentName() != null ? request.getDepartmentName() : department.getDepartmentName());
        department.setDescription(request.getDescription() != null ? request.getDescription() : department.getDescription());
        department.setStatus(request.getStatus() != null ? request.getStatus() : department.getStatus());
        if (hospital != null) {
            department.setHospital(hospital);
        }
    }

    public DepartmentResponse toResponse(DepartmentEntity department) {
        if (department == null) {
            return null;
        }
        return DepartmentResponse.builder()
                .id(department.getId())
                .departmentCode(department.getDepartmentCode())
                .departmentName(department.getDepartmentName())
                .description(department.getDescription())
                .status(department.getStatus())
                .hospitalId(department.getHospital() != null ? department.getHospital().getId() : null)
                .hospitalName(department.getHospital() != null ? department.getHospital().getHospitalName() : null)
                .message("Success")
                .build();
    }
}
