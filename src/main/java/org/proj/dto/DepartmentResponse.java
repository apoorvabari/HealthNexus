package org.proj.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.proj.entity.DepartmentEntity.DepartmentStatus;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentResponse {
    private UUID id;
    private String departmentCode;
    private String departmentName;
    private String description;
    private DepartmentStatus status;
    private UUID hospitalId;
    private String hospitalName;
    private String message;
}
