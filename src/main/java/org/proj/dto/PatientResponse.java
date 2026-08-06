package org.proj.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.proj.entity.PatientEntity.Gender;
import org.proj.entity.PatientEntity.BloodGroup;
import org.proj.entity.PatientEntity.PatientStatus;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientResponse {
    private UUID id;
    private UUID accountId;
    private String accountName;
    private UUID hospitalId;
    private String hospitalName;
    private String patientCode;
    private BloodGroup bloodGroup;
    private Gender gender;
    private LocalDate dateOfBirth;
    private String emergencyContactName;
    private String emergencyContactPhone;
    private String relationship;
    private String address;
    private String city;
    private String state;
    private String postalCode;
    private PatientStatus status;
    private String message;
}
