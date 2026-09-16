package org.proj.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "First name is required")
    @NotNull
    @Size(min = 3, max = 10, message = "First name must be between 3 and 10 characters")
    @Pattern(regexp = "^[A-Za-z]+$", message = "First name must be between 3 and 10 characters")
    private String firstName;

    @NotBlank(message = "Middle name is required")
    @NotNull
    @Pattern(regexp = "^[A-Za-z]{1,10}$", message = "Middle name must contain only alphabets and be between 1 and 10 characters")
    @Size(min = 1, max = 10)
    private String middleName;

    @NotBlank(message = "Last name is required")
    @NotNull
    @Size(min = 1, max = 10, message = "Only alphabets are allowed in last name")
    @Pattern(regexp = "^[A-Za-z]+$", message = "Only alphabets are allowed in last name")
    private String lastName;

    @NotBlank(message = "Email is required")
    @NotNull
    @Email(message = "Enter a valid email address")
    @Pattern(regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$", message = "Enter a valid email address")
    private String email;

    @NotBlank(message = "Password is required")
    @NotNull
    @Size(min = 4, max = 20, message = "Password must be between 4 and 20 characters")
    private String password;

    private String role;

    @NotBlank(message = "Please enter phone number.")
    @NotNull
    @Pattern(regexp = "^[0-9]{10}$", message = "Phone number must be exactly 10 digits")
    private String phoneNumber;

    private Boolean isActive;

    private Boolean isDeleted;

    private String profilePicture;

}
