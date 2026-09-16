package org.proj.mapper;

import org.proj.dto.PatientResponse;
import org.proj.dto.PatientRequest;
import org.proj.entity.PatientEntity;
import org.proj.entity.UserEntity;
import org.proj.entity.HospitalEntity;
import org.springframework.stereotype.Component;
import java.util.UUID;

@Component
public class PatientMapper {

    public PatientEntity toEntity(PatientRequest request, UserEntity account, HospitalEntity hospital) {
        if (request == null) {
            return null;
        }
        return PatientEntity.builder()
                .account(account)
                .hospital(hospital)
                .patientCode(request.getPatientCode())
                .bloodGroup(request.getBloodGroup())
                .gender(request.getGender())
                .dateOfBirth(request.getDateOfBirth())
                .emergencyContactName(request.getEmergencyContactName())
                .emergencyContactPhone(request.getEmergencyContactPhone())
                .relationship(request.getRelationship())
                .address(request.getAddress())
                .city(request.getCity())
                .state(request.getState())
                .postalCode(request.getPostalCode())
                .status(request.getStatus() != null ? request.getStatus() : PatientEntity.PatientStatus.ACTIVE)
                .build();
    }

    public void updateEntity(PatientEntity patient, PatientRequest request, UserEntity account, HospitalEntity hospital) {
        if (patient == null || request == null) {
            return;
        }
        if (account != null) {
            patient.setAccount(account);
        }
        if (hospital != null) {
            patient.setHospital(hospital);
        }
        patient.setPatientCode(request.getPatientCode() != null ? request.getPatientCode() : patient.getPatientCode());
        patient.setBloodGroup(request.getBloodGroup() != null ? request.getBloodGroup() : patient.getBloodGroup());
        patient.setGender(request.getGender() != null ? request.getGender() : patient.getGender());
        patient.setDateOfBirth(request.getDateOfBirth() != null ? request.getDateOfBirth() : patient.getDateOfBirth());
        patient.setEmergencyContactName(request.getEmergencyContactName() != null ? request.getEmergencyContactName() : patient.getEmergencyContactName());
        patient.setEmergencyContactPhone(request.getEmergencyContactPhone() != null ? request.getEmergencyContactPhone() : patient.getEmergencyContactPhone());
        patient.setRelationship(request.getRelationship() != null ? request.getRelationship() : patient.getRelationship());
        patient.setAddress(request.getAddress() != null ? request.getAddress() : patient.getAddress());
        patient.setCity(request.getCity() != null ? request.getCity() : patient.getCity());
        patient.setState(request.getState() != null ? request.getState() : patient.getState());
        patient.setPostalCode(request.getPostalCode() != null ? request.getPostalCode() : patient.getPostalCode());
        patient.setStatus(request.getStatus() != null ? request.getStatus() : patient.getStatus());
    }

    public PatientResponse toResponse(PatientEntity patient) {
        if (patient == null) {
            return null;
        }
        String accountName = "";
        if (patient.getAccount() != null) {
            try {
                String first = patient.getAccount().getFirstName() != null ? patient.getAccount().getFirstName() : "";
                String last = patient.getAccount().getLastName() != null ? patient.getAccount().getLastName() : "";
                accountName = (first + " " + last).trim();
            } catch (jakarta.persistence.EntityNotFoundException e) {
                
                accountName = "Unknown Account";
            }
        }
        
        UUID accountId = null;
        if (patient.getAccount() != null) {
            try {
                accountId = patient.getAccount().getId();
            } catch (jakarta.persistence.EntityNotFoundException e) {
                
            }
        }
        
        UUID hospitalId = null;
        String hospitalName = null;
        if (patient.getHospital() != null) {
            try {
                hospitalId = patient.getHospital().getId();
                hospitalName = patient.getHospital().getHospitalName();
            } catch (jakarta.persistence.EntityNotFoundException e) {
                
            }
        }
        
        String profilePicture = null;
        if (patient.getAccount() != null) {
            try {
                profilePicture = patient.getAccount().getProfilePicture();
            } catch (jakarta.persistence.EntityNotFoundException e) {
            }
        }

        return PatientResponse.builder()
                .id(patient.getId())
                .accountId(accountId)
                .accountName(accountName)
                .hospitalId(hospitalId)
                .hospitalName(hospitalName)
                .patientCode(patient.getPatientCode())
                .bloodGroup(patient.getBloodGroup())
                .gender(patient.getGender())
                .dateOfBirth(patient.getDateOfBirth())
                .emergencyContactName(patient.getEmergencyContactName())
                .emergencyContactPhone(patient.getEmergencyContactPhone())
                .relationship(patient.getRelationship())
                .address(patient.getAddress())
                .city(patient.getCity())
                .state(patient.getState())
                .postalCode(patient.getPostalCode())
                .status(patient.getStatus())
                .profilePicture(profilePicture)
                .message("Success")
                .build();
    }
}
