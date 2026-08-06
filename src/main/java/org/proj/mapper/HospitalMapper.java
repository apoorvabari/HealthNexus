package org.proj.mapper;

import org.proj.dto.HospitalRequest;
import org.proj.dto.HospitalResponse;
import org.proj.entity.HospitalEntity;
import org.springframework.stereotype.Component;

@Component
public class HospitalMapper {

    public HospitalEntity toEntity(HospitalRequest request) {
        if (request == null) {
            return null;
        }
        return HospitalEntity.builder()
                .hospitalName(request.getHospitalName())
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .address(request.getAddress())
                .city(request.getCity())
                .state(request.getState())
                .postalCode(request.getPostalCode())
                .hospitalType(request.getHospitalType())
                .registrationNumber(request.getRegistrationNumber())
                .status(request.getStatus() != null ? request.getStatus() : HospitalEntity.HospitalStatus.ACTIVE)
                .build();
    }

    public void updateEntity(HospitalEntity hospital, HospitalRequest request) {
        if (hospital == null || request == null) {
            return;
        }
        hospital.setHospitalName(request.getHospitalName() != null ? request.getHospitalName() : hospital.getHospitalName());
        hospital.setEmail(request.getEmail() != null ? request.getEmail() : hospital.getEmail());
        hospital.setPhoneNumber(request.getPhoneNumber() != null ? request.getPhoneNumber() : hospital.getPhoneNumber());
        hospital.setAddress(request.getAddress() != null ? request.getAddress() : hospital.getAddress());
        hospital.setCity(request.getCity() != null ? request.getCity() : hospital.getCity());
        hospital.setState(request.getState() != null ? request.getState() : hospital.getState());
        hospital.setPostalCode(request.getPostalCode() != null ? request.getPostalCode() : hospital.getPostalCode());
        hospital.setHospitalType(request.getHospitalType() != null ? request.getHospitalType() : hospital.getHospitalType());
        hospital.setRegistrationNumber(request.getRegistrationNumber() != null ? request.getRegistrationNumber() : hospital.getRegistrationNumber());
        hospital.setStatus(request.getStatus() != null ? request.getStatus() : hospital.getStatus());
    }

    public HospitalResponse toResponse(HospitalEntity hospital) {
        if (hospital == null) {
            return null;
        }
        return HospitalResponse.builder()
                .id(hospital.getId())
                .hospitalCode(hospital.getHospitalCode())
                .hospitalName(hospital.getHospitalName())
                .email(hospital.getEmail())
                .phoneNumber(hospital.getPhoneNumber())
                .address(hospital.getAddress())
                .city(hospital.getCity())
                .state(hospital.getState())
                .postalCode(hospital.getPostalCode())
                .hospitalType(hospital.getHospitalType())
                .registrationNumber(hospital.getRegistrationNumber())
                .status(hospital.getStatus())
                .message("Success")
                .build();
    }
}
