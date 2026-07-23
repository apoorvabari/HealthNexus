package org.proj.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountFilterRequest {
    private Long id;
    private String name;
    private String email;
    private String phoneNumber;
    private String status;
}
