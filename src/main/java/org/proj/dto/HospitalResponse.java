package org.proj.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.proj.entity.HospitalEntity.HospitalType;
import org.proj.entity.HospitalEntity.HospitalStatus;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HospitalResponse {
    private UUID id;
    private String hospitalCode;
    private String hospitalName;
    private String email;
    private String phoneNumber;
    private String address;
    private String city;
    private String state;
    private String postalCode;
    private HospitalType hospitalType;
    private String registrationNumber;
    private HospitalStatus status;
    private String message;
}
