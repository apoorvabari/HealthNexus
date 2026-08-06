package org.proj.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.Email;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserFilterRequest {
    private UUID id;
    private String firstName;
    private String middleName;
    private String lastName;
    
    @Email(message = "Invalid email address")
    private String email;
    private String phoneNumber;
    private String status;
}
