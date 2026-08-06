package org.proj.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.proj.entity.ReceptionistEntity.Shift;
import org.proj.entity.ReceptionistEntity.ReceptionistStatus;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReceptionistRequest {

    @NotNull(message = "Account ID is required")
    private UUID accountId;

    @NotNull(message = "Hospital ID is required")
    private UUID hospitalId;

    @NotNull(message = "Department ID is required")
    private UUID departmentId;

    @NotBlank(message = "Employee code is required")
    @Size(min = 3, max = 20, message = "Employee code must be between 3 and 20 characters")
    @Pattern(regexp = "^[A-Za-z0-9]+$", message = "Employee code must contain only alphanumeric characters")
    private String employeeCode;

    @NotNull(message = "Shift is required")
    private Shift shift;

    @NotNull(message = "Joining date is required")
    @PastOrPresent(message = "Joining date must be in the past or present")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate joiningDate;

    @Builder.Default
    private ReceptionistStatus status = ReceptionistStatus.ACTIVE;
}
