package org.proj.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.proj.entity.HospitalEntity.HospitalType;
import org.proj.entity.HospitalEntity.HospitalStatus;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HospitalRequest {

    @NotBlank(message = "Hospital name is required")
    private String hospitalName;

    @NotBlank(message = "Official email is required")
    @Email(message = "Invalid email address")
    private String email;

    @NotBlank(message = "Contact number is required")
    @Pattern(regexp = "^[0-9]{10}$", message = "Phone number must be exactly 10 digits")
    private String phoneNumber;

    @NotBlank(message = "Address is required")
    private String address;

    @NotBlank(message = "City is required")
    private String city;

    @NotBlank(message = "State is required")
    private String state;

    @NotBlank(message = "PIN code is required")
    @Pattern(regexp = "^[0-9]{6}$", message = "PIN code must be exactly 6 digits")
    private String postalCode;

    private Double latitude;

    private Double longitude;

    @NotNull(message = "Hospital type is required")
    private HospitalType hospitalType;

    @NotBlank(message = "Registration number is required")
    private String registrationNumber;

    @Builder.Default
    private HospitalStatus status = HospitalStatus.ACTIVE;
}
