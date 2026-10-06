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
    @Size(min = 3, max = 100, message = "Hospital name must be between 3 and 100 characters")
    private String hospitalName;

    @NotBlank(message = "Official email is required")
    @Email(regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$", message = "Please provide a valid email address")
    @Size(max = 100, message = "Email cannot exceed 100 characters")
    private String email;

    @NotBlank(message = "Contact number is required")
    @Pattern(regexp = "^[0-9]{10}$", message = "Phone number must be exactly 10 digits")
    private String phoneNumber;

    @NotBlank(message = "Address is required")
    @Size(min = 5, max = 255, message = "Address must be between 5 and 255 characters")
    private String address;

    @NotBlank(message = "City is required")
    @Size(min = 2, max = 50, message = "City must be between 2 and 50 characters")
    @Pattern(regexp = "^[a-zA-Z\\s]+$", message = "City must contain only letters")
    private String city;

    @NotBlank(message = "State is required")
    @Size(min = 2, max = 50, message = "State must be between 2 and 50 characters")
    @Pattern(regexp = "^[a-zA-Z\\s]+$", message = "State must contain only letters")
    private String state;

    @NotBlank(message = "PIN code is required")
    @Pattern(regexp = "^[1-9][0-9]{5}$", message = "PIN code must be a valid 6-digit postal code")
    private String postalCode;

    private Double latitude;

    private Double longitude;

    @NotNull(message = "Hospital type is required")
    private HospitalType hospitalType;

    @NotBlank(message = "Registration number is required")
    @Size(min = 3, max = 30, message = "Registration number must be between 3 and 30 characters")
    @Pattern(regexp = "^[a-zA-Z0-9/\\-_]+$", message = "Registration number must be alphanumeric")
    private String registrationNumber;

    @Builder.Default
    private HospitalStatus status = HospitalStatus.ACTIVE;
}
