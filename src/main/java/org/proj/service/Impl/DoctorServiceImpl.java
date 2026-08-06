package org.proj.service.Impl;

import org.proj.dto.DoctorRequest;
import org.proj.dto.DoctorResponse;
import org.proj.entity.DoctorEntity;
import org.proj.entity.UserEntity;
import org.proj.entity.HospitalEntity;
import org.proj.entity.DepartmentEntity;
import org.proj.mapper.DoctorMapper;
import org.proj.repository.DoctorRepo;
import org.proj.repository.UserRepo;
import org.proj.repository.HospitalRepo;
import org.proj.repository.DepartmentRepo;
import org.proj.service.DoctorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class DoctorServiceImpl implements DoctorService {

    @Autowired
    private DoctorRepo doctorRepo;

    @Autowired
    private UserRepo userRepo;

    @Autowired
    private HospitalRepo hospitalRepo;

    @Autowired
    private DepartmentRepo departmentRepo;

    @Autowired
    private DoctorMapper doctorMapper;

    @Override
    @Transactional
    public DoctorResponse createDoctor(DoctorRequest request) {
        try {
            if (request.getAccountId() == null) {
                throw new IllegalArgumentException("Account ID is required");
            }

            UserEntity account = userRepo.findById(request.getAccountId())
                    .orElseThrow(() -> new IllegalArgumentException("Account not found"));

            if (account.getRole() == null || !"doctor".equals(account.getRole().getRoleName())) {
                throw new IllegalArgumentException("Account is not assigned DOCTOR role.");
            }

            if (doctorRepo.existsByAccountId(request.getAccountId())) {
                throw new IllegalArgumentException("Doctor profile already exists for this account");
            }

            HospitalEntity hospital = hospitalRepo.findById(request.getHospitalId())
                    .orElseThrow(() -> new IllegalArgumentException("Hospital not found"));

            DepartmentEntity department = departmentRepo.findById(request.getDepartmentId())
                    .orElseThrow(() -> new IllegalArgumentException("Department not found"));

            if (doctorRepo.existsByLicenseNumber(request.getLicenseNumber())) {
                throw new IllegalArgumentException("Doctor with this license number already exists");
            }

            DoctorEntity doctor = doctorMapper.toEntity(request, account, hospital, department);
            DoctorEntity savedDoctor = doctorRepo.save(doctor);

            DoctorResponse response = doctorMapper.toResponse(savedDoctor);
            response.setMessage("Doctor profile created successfully");
            return response;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Unable to create doctor profile.", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public DoctorResponse getDoctorById(UUID id) {
        try {
            if (id == null) {
                throw new IllegalArgumentException("Doctor Id is required");
            }
            DoctorEntity doctor = doctorRepo.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Doctor profile not found"));
            return doctorMapper.toResponse(doctor);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Unable to fetch doctor profile.", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<DoctorResponse> getAllDoctors() {
        try {
            return doctorRepo.findAll().stream()
                    .map(doctorMapper::toResponse)
                    .toList();
        } catch (Exception e) {
            throw new RuntimeException("Unable to fetch doctor list.", e);
        }
    }

    @Override
    @Transactional
    public DoctorResponse updateDoctor(UUID id, DoctorRequest request) {
        try {
            if (id == null) {
                throw new IllegalArgumentException("Doctor Id is required");
            }
            DoctorEntity doctor = doctorRepo.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Doctor profile not found"));

            UserEntity account = doctor.getAccount();
            if (request.getAccountId() != null && !request.getAccountId().equals(account.getId())) {
                account = userRepo.findById(request.getAccountId())
                        .orElseThrow(() -> new IllegalArgumentException("Account not found"));

                if (account.getRole() == null || !"doctor".equals(account.getRole().getRoleName())) {
                    throw new IllegalArgumentException("Account is not assigned DOCTOR role.");
                }

                if (doctorRepo.existsByAccountIdAndIdNot(request.getAccountId(), id)) {
                    throw new IllegalArgumentException("Doctor profile already exists for this account");
                }
            }

            HospitalEntity hospital = doctor.getHospital();
            if (request.getHospitalId() != null && !request.getHospitalId().equals(hospital.getId())) {
                hospital = hospitalRepo.findById(request.getHospitalId())
                        .orElseThrow(() -> new IllegalArgumentException("Hospital not found"));
            }

            DepartmentEntity department = doctor.getDepartment();
            if (request.getDepartmentId() != null && !request.getDepartmentId().equals(department.getId())) {
                department = departmentRepo.findById(request.getDepartmentId())
                        .orElseThrow(() -> new IllegalArgumentException("Department not found"));
            }

            if (request.getLicenseNumber() != null
                    && !request.getLicenseNumber().equals(doctor.getLicenseNumber())) {
                if (doctorRepo.existsByLicenseNumberAndIdNot(request.getLicenseNumber(), id)) {
                    throw new IllegalArgumentException("Doctor with this license number already exists");
                }
            }

            doctorMapper.updateEntity(doctor, request, account, hospital, department);
            DoctorEntity updatedDoctor = doctorRepo.save(doctor);

            DoctorResponse response = doctorMapper.toResponse(updatedDoctor);
            response.setMessage("Doctor profile updated successfully");
            return response;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Unable to update doctor profile.", e);
        }
    }

    @Override
    @Transactional
    public void deleteDoctor(UUID id) {
        try {
            if (id == null) {
                throw new IllegalArgumentException("Doctor Id is required");
            }
            DoctorEntity doctor = doctorRepo.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Doctor profile not found"));

            doctor.setStatus(DoctorEntity.DoctorStatus.INACTIVE);
            doctorRepo.save(doctor);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Unable to delete doctor profile.", e);
        }
    }
}
