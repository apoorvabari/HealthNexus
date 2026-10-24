package org.proj.service;

import org.proj.dto.DoctorRequest;
import java.math.BigDecimal;
import org.proj.dto.DoctorResponse;
import org.proj.entity.DoctorEntity;

import java.util.UUID;
import java.util.List;
import java.util.Optional;

import org.proj.dto.PageResponse;
import org.springframework.data.domain.Page;

public interface DoctorService {

    DoctorResponse createDoctor(DoctorRequest request);

    DoctorResponse getDoctorById(UUID id);

    List<DoctorResponse> getDoctorsByDepartment(UUID departmentId);

    List<DoctorResponse> getDoctorsByHospital(UUID hospitalId);

    PageResponse<DoctorResponse> getAllDoctors(String search, int page, int size, boolean isAdmin);

    PageResponse<DoctorResponse> searchDoctorsForPatient(
        String search,
        String specialization,
        UUID hospitalId,
        UUID departmentId,
        BigDecimal maxFee,
        Integer minExperience,
        int page,
        int size
);

    PageResponse<DoctorResponse> getNearbyDoctors(
        double latitude,
        double longitude,
        double radiusKm,
        String specialization,
        BigDecimal maxFee,
        Integer minExperience,
        int page,
        int size
);

    DoctorResponse updateDoctor(UUID id, DoctorRequest request);

    void deleteDoctor(UUID id);

    DoctorEntity findDoctorById(UUID doctorId);

    java.util.Optional<DoctorEntity> findDoctorByIdOrAccountId(UUID id);

    DoctorEntity findDoctorByIdAndHospitalId(UUID doctorId, UUID hospitalId);

    java.util.Optional<DoctorEntity> findLockedByIdAndHospitalId(UUID doctorId, UUID hospitalId);

    DoctorResponse getDoctorByAccountId(UUID accountId);

    Optional<DoctorEntity> findDoctorEntityByAccountId(UUID accountId);

    Optional<DoctorEntity> findDoctorByAccountUsername(String username);

    void save(DoctorEntity doctor);

    long count();

    long countByVerificationStatus(DoctorEntity.VerificationStatus verificationStatus);

    Page<DoctorEntity> getDoctorsByDepartment(UUID departmentId, int page, int size);

    long countDoctorsByDepartment(UUID departmentId);

    long countDoctorsByDepartmentAndStatus(UUID departmentId, DoctorEntity.DoctorStatus status);

    long countByHospitalId(UUID hospitalId);

    long countByHospitalIdAndVerificationStatus(UUID hospitalId, DoctorEntity.VerificationStatus verificationStatus);

    void switchActiveHospital(UUID doctorId, UUID targetHospitalId);

    List<org.proj.dto.DoctorAffiliationResponse> getDoctorAffiliations(UUID doctorId);

    org.proj.dto.DoctorAffiliationResponse addOrUpdateAffiliation(UUID doctorId, org.proj.dto.DoctorAffiliationRequest request);

    void removeAffiliation(UUID affiliationId);
}
