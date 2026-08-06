package org.proj.dto;

import jakarta.validation.constraints.*;
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
public class DepartmentRequest {

    @NotBlank(message = "Department code is required")
    private String departmentCode;

    @NotBlank(message = "Department name is required")
    private String departmentName;

    private String description;

    @NotNull(message = "Hospital ID is required")
    private UUID hospitalId;

    @Builder.Default
    private DepartmentStatus status = DepartmentStatus.ACTIVE;
}
