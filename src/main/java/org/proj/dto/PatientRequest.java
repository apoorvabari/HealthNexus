package org.proj.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
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
public class PatientRequest {

    @NotNull(message = "Account ID is required")
    private UUID accountId;

    private UUID hospitalId;

    private String patientCode;

    @NotNull(message = "Blood group is required")
    private BloodGroup bloodGroup;

    @NotNull(message = "Gender is required")
    private Gender gender;

    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be in the past")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateOfBirth;

    @NotBlank(message = "Emergency contact name is required")
    private String emergencyContactName;

    @NotBlank(message = "Emergency contact phone is required")
    @Pattern(regexp = "^[0-9]{10}$", message = "Emergency contact phone must be exactly 10 digits")
    private String emergencyContactPhone;

    @NotBlank(message = "Emergency contact relationship is required")
    private String relationship;

    @NotBlank(message = "Address is required")
    private String address;

    @NotBlank(message = "City is required")
    private String city;

    @NotBlank(message = "State is required")
    private String state;

    @NotBlank(message = "Postal code is required")
    @Pattern(regexp = "^[0-9]{6}$", message = "Postal code must be exactly 6 digits")
    private String postalCode;

    @Builder.Default
    private PatientStatus status = PatientStatus.ACTIVE;
}
