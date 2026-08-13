package org.proj.service.Impl;

import org.proj.dto.DoctorRequest;
import org.proj.dto.DoctorResponse;
import org.proj.entity.DoctorEntity;
import org.proj.entity.UserEntity;
import org.proj.entity.HospitalEntity;
import org.proj.entity.DepartmentEntity;
import org.proj.mapper.DoctorMapper;
import org.proj.repository.DoctorRepo;
import org.proj.service.DepartmentService;
import org.proj.service.DoctorService;
import org.proj.service.HospitalService;
import org.proj.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import org.proj.dto.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

@Service
public class DoctorServiceImpl implements DoctorService {

    @Autowired
    private DoctorRepo doctorRepo;

    @Autowired
    private UserService userService;

    @Autowired
    private HospitalService hospitalService;

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private DoctorMapper doctorMapper;

    @Override
    @Transactional
    public DoctorResponse createDoctor(DoctorRequest request) {
        try {
            if (request.getAccountId() == null) {
                throw new IllegalArgumentException("Account ID is required");
            }

            UserEntity account = userService.findUserById(request.getAccountId());

            if (account.getRole() == null || !"DOCTOR".equalsIgnoreCase(account.getRole().getRoleName())) {
                throw new IllegalArgumentException("Account is not assigned DOCTOR role.");
            }

            if (doctorRepo.existsByAccountId(request.getAccountId())) {
                throw new IllegalArgumentException("Doctor profile already exists for this account");
            }

            HospitalEntity hospital = hospitalService.findHospitalById(request.getHospitalId());

            DepartmentEntity department = departmentService.findDepartmentById(request.getDepartmentId());

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
    public PageResponse<DoctorResponse> getAllDoctors(String search, int page, int size) {
        try {
            Page<DoctorEntity> doctorPage = doctorRepo.searchDoctors(search, PageRequest.of(page, size));
            List<DoctorResponse> content = doctorPage.getContent().stream()
                    .map(doctorMapper::toResponse)
                    .toList();
            return new PageResponse<>(content, doctorPage.getNumber(), doctorPage.getSize(), doctorPage.getTotalElements(), doctorPage.getTotalPages(), doctorPage.isLast());
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
                account = userService.findUserById(request.getAccountId());

                if (account.getRole() == null || !"DOCTOR".equalsIgnoreCase(account.getRole().getRoleName())) {
                    throw new IllegalArgumentException("Account is not assigned DOCTOR role.");
                }

                if (doctorRepo.existsByAccountIdAndIdNot(request.getAccountId(), id)) {
                    throw new IllegalArgumentException("Doctor profile already exists for this account");
                }
            }

            HospitalEntity hospital = doctor.getHospital();
            if (request.getHospitalId() != null && !request.getHospitalId().equals(hospital.getId())) {
                hospital = hospitalService.findHospitalById(request.getHospitalId());
            }

            DepartmentEntity department = doctor.getDepartment();
            if (request.getDepartmentId() != null && !request.getDepartmentId().equals(department.getId())) {
                department = departmentService.findDepartmentById(request.getDepartmentId());
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

    @Override
    public DoctorEntity findDoctorById(UUID doctorId) {
        return doctorRepo.findById(doctorId)
                .orElseThrow(() -> new IllegalArgumentException("Doctor not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public DoctorResponse getDoctorByAccountId(UUID accountId) {
        DoctorEntity doctor = doctorRepo.findByAccountId(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Doctor profile not found for this account"));
        return doctorMapper.toResponse(doctor);
    }

    @Override
    public void save(DoctorEntity doctor) {
        doctorRepo.save(doctor);
    }

    @Override
    public long count() {
        return doctorRepo.count();
    }
}
