package org.proj.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetRequest {

    @NotBlank(message = "Email is required")
    @NotNull
    @Email(message = "Enter a valid email address")
    private String email;

    @NotBlank(message = "Reset token is required")
    @NotNull
    private String token;

    @NotBlank(message = "New password is required")
    @NotNull
    @Size(min = 4, max = 20, message = "Password must be between 4 and 20 characters")
    private String newPassword;

    @NotBlank(message = "Confirm password is required")
    @NotNull
    private String confirmPassword;
}
