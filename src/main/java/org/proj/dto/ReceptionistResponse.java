package org.proj.dto;

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
public class ReceptionistResponse {
    private UUID id;
    private UUID accountId;
    private String accountName;
    private UUID hospitalId;
    private String hospitalName;
    private UUID departmentId;
    private String departmentName;
    private String employeeCode;
    private Shift shift;
    private LocalDate joiningDate;
    private ReceptionistStatus status;
    private String message;
}
