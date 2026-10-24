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
    @Size(min = 2, max = 50, message = "Department code must be between 2 and 50 characters")
    @Pattern(regexp = "^[a-zA-Z0-9\\-_]+$", message = "Department code must contain only alphanumeric characters, hyphens, or underscores")
    private String departmentCode;

    @NotBlank(message = "Department name is required")
    @Size(min = 2, max = 100, message = "Department name must be between 2 and 100 characters")
    private String departmentName;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    @NotNull(message = "Hospital ID is required")
    private UUID hospitalId;

    @Builder.Default
    private DepartmentStatus status = DepartmentStatus.ACTIVE;
}
