package org.proj.mapper;

import org.proj.dto.DoctorResponse;
import org.proj.dto.DoctorRequest;
import org.proj.entity.DoctorEntity;
import org.proj.entity.UserEntity;
import org.proj.entity.HospitalEntity;
import org.proj.entity.DepartmentEntity;
import org.springframework.stereotype.Component;

@Component
public class DoctorMapper {

    public DoctorEntity toEntity(DoctorRequest request, UserEntity account, HospitalEntity hospital, DepartmentEntity department) {
        if (request == null) {
            return null;
        }
        return DoctorEntity.builder()
                .account(account)
                .hospital(hospital)
                .department(department)
                .specialization(request.getSpecialization())
                .qualification(request.getQualification())
                .experience(request.getExperience())
                .consultationFee(request.getConsultationFee())
                .licenseNumber(request.getLicenseNumber())
                .status(request.getStatus() != null ? request.getStatus() : DoctorEntity.DoctorStatus.ACTIVE)
                .build();
    }

    public void updateEntity(DoctorEntity doctor, DoctorRequest request, UserEntity account, HospitalEntity hospital, DepartmentEntity department) {
        if (doctor == null || request == null) {
            return;
        }
        if (account != null) {
            doctor.setAccount(account);
        }
        if (hospital != null) {
            doctor.setHospital(hospital);
        }
        if (department != null) {
            doctor.setDepartment(department);
        }
        doctor.setSpecialization(request.getSpecialization() != null ? request.getSpecialization() : doctor.getSpecialization());
        doctor.setQualification(request.getQualification() != null ? request.getQualification() : doctor.getQualification());
        doctor.setExperience(request.getExperience() != null ? request.getExperience() : doctor.getExperience());
        doctor.setConsultationFee(request.getConsultationFee() != null ? request.getConsultationFee() : doctor.getConsultationFee());
        doctor.setLicenseNumber(request.getLicenseNumber() != null ? request.getLicenseNumber() : doctor.getLicenseNumber());
        doctor.setStatus(request.getStatus() != null ? request.getStatus() : doctor.getStatus());
    }

    public DoctorResponse toResponse(DoctorEntity doctor) {
        if (doctor == null) {
            return null;
        }
        String accountName = "";
        if (doctor.getAccount() != null) {
            String first = doctor.getAccount().getFirstName() != null ? doctor.getAccount().getFirstName() : "";
            String last = doctor.getAccount().getLastName() != null ? doctor.getAccount().getLastName() : "";
            accountName = (first + " " + last).trim();
        }
        return DoctorResponse.builder()
                .id(doctor.getId())
                .accountId(doctor.getAccount() != null ? doctor.getAccount().getId() : null)
                .accountName(accountName)
                .hospitalId(doctor.getHospital() != null ? doctor.getHospital().getId() : null)
                .hospitalName(doctor.getHospital() != null ? doctor.getHospital().getHospitalName() : null)
                .departmentId(doctor.getDepartment() != null ? doctor.getDepartment().getId() : null)
                .departmentName(doctor.getDepartment() != null ? doctor.getDepartment().getDepartmentName() : null)
                .specialization(doctor.getSpecialization())
                .qualification(doctor.getQualification())
                .experience(doctor.getExperience())
                .consultationFee(doctor.getConsultationFee())
                .licenseNumber(doctor.getLicenseNumber())
                .status(doctor.getStatus())
                .message("Success")
                .build();
    }
}
