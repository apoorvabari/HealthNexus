package org.proj.service.impl;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.proj.dto.DoctorAffiliationApplicationRequest;
import org.proj.dto.DoctorAffiliationApplicationResponse;
import org.proj.dto.DoctorAffiliationRequest;
import org.proj.dto.DoctorAffiliationResponse;
import org.proj.dto.ReviewAffiliationRequest;
import org.proj.entity.AdminEntity;
import org.proj.entity.DepartmentEntity;
import org.proj.entity.DoctorAffiliationRequestEntity;
import org.proj.entity.DoctorAffiliationRequestEntity.RequestStatus;
import org.proj.entity.DoctorEntity;
import org.proj.entity.DoctorHospitalAffiliationEntity;
import org.proj.entity.HospitalEntity;
import org.proj.entity.NotificationEntity.NotificationType;
import org.proj.entity.UserEntity;
import org.proj.repository.DoctorHospitalAffiliationRepo;
import org.proj.security.SecurityUtils;
import org.proj.service.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor

public class DoctorHospitalAffiliationServiceImpl implements DoctorHospitalAffiliationService {

    private final DoctorHospitalAffiliationRepo affiliationRepo;
    private final DoctorAffiliationRequestService affiliationRequestService;
    private final DoctorService doctorService;
    private final HospitalService hospitalService;
    private final DepartmentService departmentService;
    private final AdminService adminService;
    private final NotificationService notificationService;

    @Override
    @Transactional(readOnly = true)
    public Optional<DoctorHospitalAffiliationEntity> findActiveDutyAffiliation(UUID doctorId) {
        try {
            return doctorId == null ? Optional.empty() : affiliationRepo.findByDoctorIdAndActiveDutyTrue(doctorId);
        } catch (Exception e) {
            String msg = e.getMessage() != null && !e.getMessage().isBlank() ? e.getMessage()
                    : "Unable to find active duty affiliation";
            throw new RuntimeException(msg, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DoctorHospitalAffiliationEntity> findAffiliation(UUID doctorId, UUID hospitalId) {
        try {
            return (doctorId == null || hospitalId == null)
                    ? Optional.empty()
                    : affiliationRepo.findByDoctorIdAndHospitalId(doctorId, hospitalId);
        } catch (Exception e) {
            String msg = e.getMessage() != null && !e.getMessage().isBlank() ? e.getMessage()
                    : "Unable to find affiliation";
            throw new RuntimeException(msg, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasAffiliation(UUID doctorId, UUID hospitalId) {
        try {
            return (doctorId == null || hospitalId == null)
                    ? false
                    : affiliationRepo.findByDoctorIdAndHospitalId(doctorId, hospitalId).isPresent();
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<DoctorAffiliationResponse> getDoctorAffiliations(UUID doctorId) {
        try {
            if (doctorId == null) {
                throw new IllegalArgumentException("Doctor ID is required");
            }
            List<DoctorHospitalAffiliationEntity> list = affiliationRepo.findByDoctorId(doctorId);
            return list != null ? list.stream().map(this::mapToAffiliationResponse).toList() : List.of();
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            String msg = e.getMessage() != null && !e.getMessage().isBlank() ? e.getMessage()
                    : "Unable to fetch doctor affiliations";
            throw new RuntimeException(msg, e);
        }
    }

    @Override
    @Transactional
    public DoctorAffiliationResponse addOrUpdateAffiliation(UUID doctorId, DoctorAffiliationRequest request) {
        try {
            if (doctorId == null || request == null) {
                throw new IllegalArgumentException("Doctor ID and request are required");
            }

            DoctorEntity doctor = doctorService.findDoctorById(doctorId);
            HospitalEntity hospital = hospitalService.findHospitalById(request.getHospitalId());
            DepartmentEntity department = departmentService.findDepartmentById(request.getDepartmentId());

            if (!hospital.getId().equals(department.getHospital().getId())) {
                throw new IllegalArgumentException("Department does not belong to the selected hospital");
            }

            DoctorHospitalAffiliationEntity affiliation = affiliationRepo
                    .findByDoctorIdAndHospitalId(doctorId, hospital.getId())
                    .orElse(DoctorHospitalAffiliationEntity.builder()
                            .doctor(doctor)
                            .hospital(hospital)
                            .build());

            affiliation.setDepartment(department);
            if (request.getConsultationFee() != null) {
                affiliation.setConsultationFee(request.getConsultationFee());
            }
            if (request.getStatus() != null) {
                affiliation.setStatus(request.getStatus());
            }
            if (Boolean.TRUE.equals(request.getActiveDuty())) {
                affiliationRepo.resetActiveDutyForDoctor(doctorId);
                affiliation.setActiveDuty(true);
                doctor.setHospital(hospital);
                doctor.setDepartment(department);
                doctorService.save(doctor);
            }

            DoctorHospitalAffiliationEntity saved = affiliationRepo.save(affiliation);
            return mapToAffiliationResponse(saved);
        } catch (AccessDeniedException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            String msg = e.getMessage() != null && !e.getMessage().isBlank() ? e.getMessage()
                    : "Unable to add or update affiliation";
            throw new RuntimeException(msg, e);
        }
    }

    @Override
    @Transactional
    public void switchActiveHospital(UUID doctorId, UUID targetHospitalId) {
        try {
            if (doctorId == null || targetHospitalId == null) {
                throw new IllegalArgumentException("Doctor ID and target hospital ID are required");
            }

            DoctorEntity doctor = doctorService.findDoctorById(doctorId);

            // Single-Admin Security Boundary Check
            if (doctor.getAccount() != null) {
                boolean isSameAdmin = affiliationRepo.isDoctorAndHospitalUnderSameAdmin(doctor.getAccount().getId(),
                        targetHospitalId);
                if (!isSameAdmin) {
                    throw new AccessDeniedException(
                            "Security Violation: Target hospital does not belong to your administrative domain.");
                }
            }

            DoctorHospitalAffiliationEntity targetAffiliation = affiliationRepo
                    .findByDoctorIdAndHospitalId(doctorId, targetHospitalId)
                    .orElseThrow(() -> new IllegalArgumentException("Doctor is not affiliated with this hospital"));

            if (targetAffiliation.getStatus() != DoctorHospitalAffiliationEntity.AffiliationStatus.ACTIVE) {
                throw new IllegalStateException(
                        "Cannot switch active duty: Affiliation status is " + targetAffiliation.getStatus());
            }

            // Atomically clear activeDuty for other hospitals and activate the target
            affiliationRepo.resetActiveDutyForDoctor(doctorId);
            targetAffiliation.setActiveDuty(true);
            affiliationRepo.save(targetAffiliation);

            // Update doctor core pointers for seamless backward compatibility
            doctor.setHospital(targetAffiliation.getHospital());
            doctor.setDepartment(targetAffiliation.getDepartment());
            doctorService.save(doctor);
        } catch (AccessDeniedException | IllegalArgumentException | IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            String msg = e.getMessage() != null && !e.getMessage().isBlank() ? e.getMessage()
                    : "Unable to switch active hospital";
            throw new RuntimeException(msg, e);
        }
    }

    @Override
    @Transactional
    public void removeAffiliation(UUID affiliationId) {
        try {
            if (affiliationId == null) {
                throw new IllegalArgumentException("Affiliation ID is required");
            }
            affiliationRepo.deleteById(affiliationId);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            String msg = e.getMessage() != null && !e.getMessage().isBlank() ? e.getMessage()
                    : "Unable to remove affiliation";
            throw new RuntimeException(msg, e);
        }
    }

    // --- Request & Approval Pipeline ---

    @Override
    @Transactional
    public DoctorAffiliationApplicationResponse submitAffiliationRequest(DoctorAffiliationApplicationRequest request) {
        try {
            if (request == null || request.getHospitalId() == null || request.getDepartmentId() == null) {
                throw new IllegalArgumentException("Hospital and Department are required");
            }

            UserEntity currentUser = SecurityUtils.getCurrentUser();
            DoctorEntity doctor = doctorService.findDoctorEntityByAccountId(currentUser.getId())
                    .orElseThrow(() -> new EntityNotFoundException("Doctor profile not found for current user"));

            HospitalEntity targetHospital = hospitalService.findHospitalById(request.getHospitalId());
            DepartmentEntity targetDepartment = departmentService.findDepartmentById(request.getDepartmentId());

            if (!targetHospital.getId().equals(targetDepartment.getHospital().getId())) {
                throw new IllegalArgumentException("Department does not belong to the selected hospital");
            }

            // Guard: Verify target hospital and doctor share admin network
            boolean isSameAdmin = affiliationRepo.isDoctorAndHospitalUnderSameAdmin(doctor.getAccount().getId(),
                    targetHospital.getId());
            if (!isSameAdmin) {
                throw new AccessDeniedException(
                        "Cannot request affiliation with a hospital outside your administrator network");
            }

            // Avoid duplicate active affiliations
            boolean alreadyAffiliated = affiliationRepo
                    .findByDoctorIdAndHospitalId(doctor.getId(), targetHospital.getId()).isPresent();
            if (alreadyAffiliated) {
                throw new IllegalArgumentException("You are already affiliated with this hospital");
            }

            // Avoid duplicate pending requests
            boolean hasPendingRequest = affiliationRequestService.existsByDoctorIdAndTargetHospitalIdAndStatus(
                    doctor.getId(), targetHospital.getId(), RequestStatus.PENDING);
            if (hasPendingRequest) {
                throw new IllegalArgumentException("You already have a pending affiliation request for this hospital");
            }

            String sanitizedReason = request.getReason() != null ? request.getReason().trim() : null;
            if (sanitizedReason != null && sanitizedReason.length() > 500) {
                throw new IllegalArgumentException("Reason cannot exceed 500 characters");
            }

            DoctorAffiliationRequestEntity appRequest = DoctorAffiliationRequestEntity.builder()
                    .doctor(doctor)
                    .targetHospital(targetHospital)
                    .targetDepartment(targetDepartment)
                    .reason(sanitizedReason)
                    .status(RequestStatus.PENDING)
                    .build();

            DoctorAffiliationRequestEntity saved = affiliationRequestService.save(appRequest);

            // Notify Admins for this hospital
            List<AdminEntity> admins = adminService.findAdminsByHospitalId(targetHospital.getId());
            if (admins != null) {
                for (AdminEntity admin : admins) {
                    if (admin.getAccount() != null) {
                        try {
                            notificationService.createNotification(
                                    admin.getAccount(),
                                    "New Doctor Affiliation Request",
                                    "Dr. " + (doctor.getAccount() != null ? doctor.getAccount().getFullName()
                                            : "Doctor")
                                            + " requested affiliation with " + targetHospital.getHospitalName()
                                            + " (" + targetDepartment.getDepartmentName() + ")",
                                    NotificationType.GENERAL);
                        } catch (Exception notifEx) {
                            // Non-critical notification failure
                        }
                    }
                }
            }

            return mapToApplicationResponse(saved);
        } catch (AccessDeniedException | IllegalArgumentException | EntityNotFoundException e) {
            throw e;
        } catch (Exception e) {
            String msg = e.getMessage() != null && !e.getMessage().isBlank() ? e.getMessage()
                    : "Unable to submit affiliation request";
            throw new RuntimeException(msg, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<DoctorAffiliationApplicationResponse> getMyAffiliationRequests() {
        try {
            UserEntity currentUser = SecurityUtils.getCurrentUser();
            DoctorEntity doctor = doctorService.findDoctorEntityByAccountId(currentUser.getId())
                    .orElseThrow(() -> new EntityNotFoundException("Doctor profile not found"));

            List<DoctorAffiliationRequestEntity> list = affiliationRequestService
                    .findByDoctorIdOrderByCreatedAtDesc(doctor.getId());
            return list != null ? list.stream().map(this::mapToApplicationResponse).toList() : List.of();
        } catch (AccessDeniedException | EntityNotFoundException e) {
            throw e;
        } catch (Exception e) {
            String msg = e.getMessage() != null && !e.getMessage().isBlank() ? e.getMessage()
                    : "Unable to fetch affiliation requests";
            throw new RuntimeException(msg, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<DoctorAffiliationApplicationResponse> getPendingRequestsForAdmin(UUID hospitalId,
            RequestStatus status) {
        try {
            UserEntity currentUser = SecurityUtils.getCurrentUser();
            List<UUID> adminHospitalIds = adminService.getActiveHospitalIdsForAdmin(currentUser.getId());

            if (adminHospitalIds == null || adminHospitalIds.isEmpty()) {
                return List.of();
            }

            if (hospitalId != null) {
                if (!adminHospitalIds.contains(hospitalId)) {
                    throw new AccessDeniedException("You do not manage this hospital");
                }
                List<DoctorAffiliationRequestEntity> list = status != null
                        ? affiliationRequestService.findByTargetHospitalIdAndStatusOrderByCreatedAtDesc(hospitalId,
                                status)
                        : affiliationRequestService.findByTargetHospitalIdOrderByCreatedAtDesc(hospitalId);
                return list != null ? list.stream().map(this::mapToApplicationResponse).toList() : List.of();
            }

            List<DoctorAffiliationRequestEntity> list = status != null
                    ? affiliationRequestService.findByTargetHospitalIdsAndStatus(adminHospitalIds, status)
                    : affiliationRequestService.findByTargetHospitalIds(adminHospitalIds);
            return list != null ? list.stream().map(this::mapToApplicationResponse).toList() : List.of();
        } catch (AccessDeniedException e) {
            throw e;
        } catch (Exception e) {
            String msg = e.getMessage() != null && !e.getMessage().isBlank() ? e.getMessage()
                    : "Unable to fetch pending requests for admin";
            throw new RuntimeException(msg, e);
        }
    }

    @Override
    @Transactional
    public DoctorAffiliationApplicationResponse reviewAffiliationRequest(UUID requestId,
            ReviewAffiliationRequest review) {
        try {
            if (requestId == null || review == null || review.getApproved() == null) {
                throw new IllegalArgumentException("Request ID and approval status are required");
            }

            DoctorAffiliationRequestEntity appRequest = affiliationRequestService.findById(requestId)
                    .orElseThrow(() -> new EntityNotFoundException("Affiliation request not found"));

            if (appRequest.getStatus() != RequestStatus.PENDING) {
                throw new IllegalStateException(
                        "Affiliation request has already been processed: " + appRequest.getStatus());
            }

            // Verify current admin manages this target hospital
            UserEntity currentUser = SecurityUtils.getCurrentUser();
            boolean managesHospital = adminService.existsByAccountIdAndHospitalId(currentUser.getId(),
                    appRequest.getTargetHospital().getId());
            if (!managesHospital) {
                throw new AccessDeniedException("You are not authorized to review requests for this hospital");
            }

            appRequest.setReviewedAt(LocalDateTime.now());
            String sanitizedNotes = review.getAdminNotes() != null ? review.getAdminNotes().trim() : null;
            if (sanitizedNotes != null && sanitizedNotes.length() > 500) {
                throw new IllegalArgumentException("Admin notes cannot exceed 500 characters");
            }
            appRequest.setAdminNotes(sanitizedNotes);

            if (Boolean.TRUE.equals(review.getApproved())) {
                appRequest.setStatus(RequestStatus.APPROVED);

                // Create active affiliation record in DB
                DoctorHospitalAffiliationEntity affiliation = DoctorHospitalAffiliationEntity.builder()
                        .doctor(appRequest.getDoctor())
                        .hospital(appRequest.getTargetHospital())
                        .department(appRequest.getTargetDepartment())
                        .status(DoctorHospitalAffiliationEntity.AffiliationStatus.ACTIVE)
                        .activeDuty(false) // doctor clocks in / switches explicitly
                        .build();

                affiliationRepo.save(affiliation);

                // Notify Doctor
                if (appRequest.getDoctor().getAccount() != null) {
                    try {
                        notificationService.createNotification(
                                appRequest.getDoctor().getAccount(),
                                "Affiliation Approved",
                                "Your affiliation with " + appRequest.getTargetHospital().getHospitalName()
                                        + " (" + appRequest.getTargetDepartment().getDepartmentName()
                                        + ") has been approved by admin.",
                                NotificationType.GENERAL);
                    } catch (Exception notifEx) {
                        // Non-critical notification failure
                    }
                }
            } else {
                appRequest.setStatus(RequestStatus.REJECTED);

                // Notify Doctor of Rejection
                if (appRequest.getDoctor().getAccount() != null) {
                    try {
                        notificationService.createNotification(
                                appRequest.getDoctor().getAccount(),
                                "Affiliation Request Rejected",
                                "Your affiliation request for " + appRequest.getTargetHospital().getHospitalName()
                                        + " was rejected. Reason: "
                                        + (review.getAdminNotes() != null ? review.getAdminNotes()
                                                : "Administrative decision"),
                                NotificationType.GENERAL);
                    } catch (Exception notifEx) {
                        // Non-critical notification failure
                    }
                }
            }

            DoctorAffiliationRequestEntity saved = affiliationRequestService.save(appRequest);
            return mapToApplicationResponse(saved);
        } catch (AccessDeniedException | IllegalArgumentException | EntityNotFoundException | IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            String msg = e.getMessage() != null && !e.getMessage().isBlank() ? e.getMessage()
                    : "Unable to review affiliation request";
            throw new RuntimeException(msg, e);
        }
    }

    private DoctorAffiliationResponse mapToAffiliationResponse(DoctorHospitalAffiliationEntity aff) {
        return DoctorAffiliationResponse.builder()
                .id(aff.getId())
                .doctorId(aff.getDoctor().getId())
                .doctorName(aff.getDoctor().getAccount() != null ? aff.getDoctor().getAccount().getFullName() : null)
                .hospitalId(aff.getHospital().getId())
                .hospitalName(aff.getHospital().getHospitalName())
                .hospitalAddress(aff.getHospital().getAddress())
                .hospitalCity(aff.getHospital().getCity())
                .departmentId(aff.getDepartment().getId())
                .departmentName(aff.getDepartment().getDepartmentName())
                .consultationFee(aff.getConsultationFee())
                .activeDuty(aff.isActiveDuty())
                .status(aff.getStatus())
                .createdAt(aff.getCreatedAt())
                .build();
    }

    private DoctorAffiliationApplicationResponse mapToApplicationResponse(DoctorAffiliationRequestEntity req) {
        return DoctorAffiliationApplicationResponse.builder()
                .id(req.getId())
                .doctorId(req.getDoctor().getId())
                .doctorName(req.getDoctor().getAccount() != null ? req.getDoctor().getAccount().getFullName() : null)
                .doctorSpecialization(req.getDoctor().getSpecialization())
                .hospitalId(req.getTargetHospital().getId())
                .hospitalName(req.getTargetHospital().getHospitalName())
                .departmentId(req.getTargetDepartment().getId())
                .departmentName(req.getTargetDepartment().getDepartmentName())
                .status(req.getStatus())
                .reason(req.getReason())
                .adminNotes(req.getAdminNotes())
                .createdAt(req.getCreatedAt())
                .reviewedAt(req.getReviewedAt())
                .build();
    }
}
