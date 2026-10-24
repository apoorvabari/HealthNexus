package org.proj.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoctorLocationConflictResponse {
    private String errorCode; // "DOCTOR_LOCATION_MISMATCH"
    private String message;
    private UUID doctorId;
    private String doctorName;
    private UUID requestedHospitalId;
    private String requestedHospitalName;
    private UUID activeHospitalId;
    private String activeHospitalName;
    private UUID activeDepartmentId;
    private String activeDepartmentName;
    private String receptionistPhone;
    private String receptionistEmail;
}
