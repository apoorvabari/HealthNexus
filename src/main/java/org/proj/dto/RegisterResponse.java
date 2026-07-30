package org.proj.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterResponse {
    private UUID id;
    private UUID userId;
    private String firstName;
    private String middleName;
    private String lastName;
    private String email;
    private String role;
    private String phoneNumber;
    private Boolean isActive;
    private Boolean isDeleted;
    private LocalDateTime lastLogin;
    private String message;
}
