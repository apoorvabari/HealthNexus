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
public class LoginResponse {
    private UUID id;
    private UUID userId;
    private String firstName;
    private String lastName;
    private String email;
    private String role;
    private String accessToken;
    private String refreshToken;
    private String message;
}
