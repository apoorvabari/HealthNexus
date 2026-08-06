package org.proj.mapper;

import org.proj.dto.ReceptionistResponse;
import org.proj.dto.ReceptionistRequest;
import org.proj.entity.ReceptionistEntity;
import org.proj.entity.UserEntity;
import org.proj.entity.HospitalEntity;
import org.proj.entity.DepartmentEntity;
import org.springframework.stereotype.Component;

@Component
public class ReceptionistMapper {

    public ReceptionistEntity toEntity(ReceptionistRequest request, UserEntity account, HospitalEntity hospital, DepartmentEntity department) {
        if (request == null) {
            return null;
        }
        return ReceptionistEntity.builder()
                .account(account)
                .hospital(hospital)
                .department(department)
                .employeeCode(request.getEmployeeCode())
                .shift(request.getShift())
                .joiningDate(request.getJoiningDate())
                .status(request.getStatus() != null ? request.getStatus() : ReceptionistEntity.ReceptionistStatus.ACTIVE)
                .build();
    }

    public void updateEntity(ReceptionistEntity receptionist, ReceptionistRequest request, UserEntity account, HospitalEntity hospital, DepartmentEntity department) {
        if (receptionist == null || request == null) {
            return;
        }
        if (account != null) {
            receptionist.setAccount(account);
        }
        if (hospital != null) {
            receptionist.setHospital(hospital);
        }
        if (department != null) {
            receptionist.setDepartment(department);
        }
        receptionist.setEmployeeCode(request.getEmployeeCode() != null ? request.getEmployeeCode() : receptionist.getEmployeeCode());
        receptionist.setShift(request.getShift() != null ? request.getShift() : receptionist.getShift());
        receptionist.setJoiningDate(request.getJoiningDate() != null ? request.getJoiningDate() : receptionist.getJoiningDate());
        receptionist.setStatus(request.getStatus() != null ? request.getStatus() : receptionist.getStatus());
    }

    public ReceptionistResponse toResponse(ReceptionistEntity receptionist) {
        if (receptionist == null) {
            return null;
        }
        String accountName = "";
        if (receptionist.getAccount() != null) {
            String first = receptionist.getAccount().getFirstName() != null ? receptionist.getAccount().getFirstName() : "";
            String last = receptionist.getAccount().getLastName() != null ? receptionist.getAccount().getLastName() : "";
            accountName = (first + " " + last).trim();
        }
        return ReceptionistResponse.builder()
                .id(receptionist.getId())
                .accountId(receptionist.getAccount() != null ? receptionist.getAccount().getId() : null)
                .accountName(accountName)
                .hospitalId(receptionist.getHospital() != null ? receptionist.getHospital().getId() : null)
                .hospitalName(receptionist.getHospital() != null ? receptionist.getHospital().getHospitalName() : null)
                .departmentId(receptionist.getDepartment() != null ? receptionist.getDepartment().getId() : null)
                .departmentName(receptionist.getDepartment() != null ? receptionist.getDepartment().getDepartmentName() : null)
                .employeeCode(receptionist.getEmployeeCode())
                .shift(receptionist.getShift())
                .joiningDate(receptionist.getJoiningDate())
                .status(receptionist.getStatus())
                .message("Success")
                .build();
    }
}
