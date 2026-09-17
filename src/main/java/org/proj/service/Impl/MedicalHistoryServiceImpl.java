package org.proj.service.impl;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.proj.dto.ConsultationResponse;
import org.proj.dto.MedicalHistoryResponse;
import org.proj.dto.MedicalRecordResponse;
import org.proj.dto.PrescriptionResponse;
import org.proj.entity.AppointmentEntity;
import org.proj.entity.ConsultationEntity;
import org.proj.entity.DoctorEntity;
import org.proj.entity.MedicalRecordEntity;
import org.proj.entity.PatientEntity;
import org.proj.entity.PrescriptionEntity;
import org.proj.entity.UserEntity;
import org.proj.mapper.MedicalRecordMapper;
import org.proj.mapper.PrescriptionMapper;
import org.proj.repository.AppointmentRepo;
import org.proj.repository.ConsultationRepo;
import org.proj.repository.DoctorRepo;
import org.proj.repository.MedicalRecordRepo;
import org.proj.repository.PatientRepo;
import org.proj.repository.PrescriptionRepo;
import org.proj.security.SecurityUtils;
import org.proj.service.MedicalHistoryService;
import org.proj.service.TenantContextService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MedicalHistoryServiceImpl implements MedicalHistoryService {

    private final PatientRepo patientRepo;
    private final DoctorRepo doctorRepo;
    private final AppointmentRepo appointmentRepo;
    private final ConsultationRepo consultationRepo;
    private final MedicalRecordRepo medicalRecordRepo;
    private final PrescriptionRepo prescriptionRepo;
    private final MedicalRecordMapper medicalRecordMapper;
    private final PrescriptionMapper prescriptionMapper;
    private final TenantContextService tenantContextService;

    @Override
    @Transactional(readOnly = true)
    public List<MedicalHistoryResponse> getPatientMedicalHistory(
            UUID patientId) {

        if (patientId == null) {
            throw new IllegalArgumentException(
                    "Patient ID is required"
            );
        }

        UUID hospitalId = requireCurrentHospital();
        UserEntity currentUser = requireCurrentUser();
        String role = getRole(currentUser);

        PatientEntity patient = patientRepo.findById(patientId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Patient not found"
                        ));

        /*
         * Important:
         * First resolve the patient globally so that an existing patient
         * belonging to another hospital results in 403 rather than 404.
         */
        validatePatientHospital(patient, hospitalId);
        validatePatientAccess(patient, currentUser, role);

        List<AppointmentEntity> appointments =
                appointmentRepo.findByPatientIdAndHospitalId(
                        patientId,
                        hospitalId
                );

        /*
         * Doctors may only see the medical history generated from their
         * own appointments. This prevents a doctor from using the patientId
         * path variable to access another doctor's clinical history.
         */
        if ("DOCTOR".equals(role)) {

            DoctorEntity currentDoctor =
                    findCurrentDoctor(
                            currentUser,
                            hospitalId
                    );

            appointments = appointments.stream()
                    .filter(appointment ->
                            appointment.getDoctor() != null
                                    && currentDoctor.getId().equals(
                                    appointment.getDoctor().getId()
                            ))
                    .toList();
        }

        appointments = appointments.stream()
                .sorted(
                        Comparator
                                .comparing(
                                        AppointmentEntity::getAppointmentDate,
                                        Comparator.nullsLast(
                                                Comparator.naturalOrder()
                                        )
                                )
                                .thenComparing(
                                        AppointmentEntity::getAppointmentTime,
                                        Comparator.nullsLast(
                                                Comparator.naturalOrder()
                                        )
                                )
                                .reversed()
                )
                .toList();

        List<MedicalHistoryResponse> history =
                new ArrayList<>();

        int visitNumber = 1;

        for (AppointmentEntity appointment : appointments) {

            /*
             * Every child lookup is tenant-scoped through the appointment's
             * hospital relationship.
             */
            ConsultationEntity consultation =
                    consultationRepo
                            .findByAppointmentIdAndAppointmentHospitalId(
                                    appointment.getId(),
                                    hospitalId
                            )
                            .orElse(null);

            MedicalRecordEntity medicalRecord =
                    medicalRecordRepo
                            .findByAppointmentIdAndAppointmentHospitalId(
                                    appointment.getId(),
                                    hospitalId
                            )
                            .stream()
                            .findFirst()
                            .orElse(null);

            List<PrescriptionEntity> prescriptions =
                    prescriptionRepo
                            .findByAppointmentIdAndAppointmentHospitalId(
                                    appointment.getId(),
                                    hospitalId
                            );

            /*
             * An appointment becomes medical history only when clinical
             * information exists for that visit.
             */
            if (consultation == null
                    && medicalRecord == null
                    && prescriptions.isEmpty()) {
                continue;
            }

            DoctorEntity doctor =
                    appointment.getDoctor();

            history.add(
                    MedicalHistoryResponse.builder()
                            .visitNumber(visitNumber++)
                            .visitId(
                                    consultation != null
                                            ? consultation.getId()
                                            : null
                            )
                            .appointmentId(
                                    appointment.getId()
                            )
                            .appointmentNumber(
                                    appointment.getAppointmentNumber()
                            )
                            .visitDate(
                                    appointment.getAppointmentDate()
                            )
                            .doctorId(
                                    doctor != null
                                            ? doctor.getId()
                                            : null
                            )
                            .doctorName(
                                    getDoctorName(doctor)
                            )
                            .doctorSpecialization(
                                    doctor != null
                                            ? doctor.getSpecialization()
                                            : null
                            )
                            .consultation(
                                    consultation != null
                                            ? mapConsultation(
                                            consultation
                                    )
                                            : null
                            )
                            .medicalRecord(
                                    medicalRecord != null
                                            ? medicalRecordMapper
                                            .toResponse(
                                                    medicalRecord
                                            )
                                            : null
                            )
                            .prescriptions(
                                    prescriptions.stream()
                                            .map(
                                                    prescriptionMapper
                                                            ::toResponse
                                            )
                                            .toList()
                            )
                            .build()
            );
        }

        return history;
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] generateMedicalHistoryPdf(
            UUID patientId) {

        if (patientId == null) {
            throw new IllegalArgumentException(
                    "Patient ID is required"
            );
        }

        UUID hospitalId = requireCurrentHospital();
        UserEntity currentUser = requireCurrentUser();
        String role = getRole(currentUser);

        PatientEntity patient = patientRepo.findById(patientId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Patient not found"
                        ));

        validatePatientHospital(patient, hospitalId);
        validatePatientAccess(patient, currentUser, role);

        List<MedicalHistoryResponse> history =
                getPatientMedicalHistory(patientId);

        try (ByteArrayOutputStream outputStream =
                     new ByteArrayOutputStream()) {

            Document document = new Document();

            PdfWriter.getInstance(
                    document,
                    outputStream
            );

            document.open();

            Font titleFont =
                    new Font(
                            Font.HELVETICA,
                            18,
                            Font.BOLD
                    );

            Font headingFont =
                    new Font(
                            Font.HELVETICA,
                            12,
                            Font.BOLD
                    );

            Font normalFont =
                    new Font(
                            Font.HELVETICA,
                            10,
                            Font.NORMAL
                    );

            Paragraph title =
                    new Paragraph(
                            "HealthNexus Medical History",
                            titleFont
                    );

            title.setAlignment(
                    Element.ALIGN_CENTER
            );

            document.add(title);
            document.add(new Paragraph(" "));

            document.add(
                    new Paragraph(
                            "Patient: "
                                    + getPatientName(patient),
                            normalFont
                    )
            );

            document.add(
                    new Paragraph(
                            "Patient Code: "
                                    + safe(
                                    patient.getPatientCode()
                            ),
                            normalFont
                    )
            );

            document.add(
                    new Paragraph(
                            "Date of Birth: "
                                    + safe(
                                    patient.getDateOfBirth()
                            ),
                            normalFont
                    )
            );

            document.add(new Paragraph(" "));

            if (history.isEmpty()) {

                document.add(
                        new Paragraph(
                                "No medical history available.",
                                normalFont
                        )
                );

            } else {

                for (MedicalHistoryResponse visit : history) {

                    document.add(
                            new Paragraph(
                                    "Visit "
                                            + visit.getVisitNumber(),
                                    headingFont
                            )
                    );

                    document.add(
                            new Paragraph(
                                    "Date: "
                                            + safe(
                                            visit.getVisitDate()
                                    ),
                                    normalFont
                            )
                    );

                    document.add(
                            new Paragraph(
                                    "Appointment: "
                                            + safe(
                                            visit.getAppointmentNumber()
                                    ),
                                    normalFont
                            )
                    );

                    document.add(
                            new Paragraph(
                                    "Doctor: "
                                            + safe(
                                            visit.getDoctorName()
                                    ),
                                    normalFont
                            )
                    );

                    document.add(
                            new Paragraph(
                                    "Specialization: "
                                            + safe(
                                            visit
                                                    .getDoctorSpecialization()
                                    ),
                                    normalFont
                            )
                    );

                    ConsultationResponse consultation =
                            visit.getConsultation();

                    if (consultation != null) {

                        document.add(
                                new Paragraph(
                                        "Consultation Status: "
                                                + safe(
                                                consultation.getStatus()
                                        ),
                                        normalFont
                                )
                        );

                        document.add(
                                new Paragraph(
                                        "Consultation Remarks: "
                                                + safe(
                                                consultation.getRemarks()
                                        ),
                                        normalFont
                                )
                        );
                    }

                    MedicalRecordResponse record =
                            visit.getMedicalRecord();

                    if (record != null) {

                        document.add(
                                new Paragraph(
                                        "Diagnosis: "
                                                + safe(
                                                record.getDiagnosis()
                                        ),
                                        normalFont
                                )
                        );

                        document.add(
                                new Paragraph(
                                        "Medical Notes: "
                                                + safe(
                                                record.getNotes()
                                        ),
                                        normalFont
                                )
                        );
                    }

                    List<PrescriptionResponse> prescriptions =
                            visit.getPrescriptions();

                    if (prescriptions != null
                            && !prescriptions.isEmpty()) {

                        document.add(
                                new Paragraph(
                                        "Prescriptions",
                                        headingFont
                                )
                        );

                        for (PrescriptionResponse prescription :
                                prescriptions) {

                            document.add(
                                    new Paragraph(
                                            "Instructions: "
                                                    + safe(
                                                    prescription
                                                            .getInstructions()
                                            ),
                                            normalFont
                                    )
                            );

                            if (prescription.getMedicines()
                                    != null) {

                                prescription
                                        .getMedicines()
                                        .forEach(medicine ->
                                                document.add(
                                                        new Paragraph(
                                                                "- "
                                                                        + safe(
                                                                        medicine
                                                                                .getMedicineName()
                                                                )
                                                                        + " | Dosage: "
                                                                        + safe(
                                                                        medicine
                                                                                .getDosage()
                                                                )
                                                                        + " | Frequency: "
                                                                        + safe(
                                                                        medicine
                                                                                .getFrequency()
                                                                )
                                                                        + " | Duration: "
                                                                        + safe(
                                                                        medicine
                                                                                .getDuration()
                                                                ),
                                                                normalFont
                                                        )
                                                )
                                        );
                            }
                        }
                    }

                    document.add(
                            new Paragraph(" ")
                    );
                }
            }

            document.close();

            return outputStream.toByteArray();

        } catch (DocumentException | IOException e) {

            throw new IllegalStateException(
                    "Unable to generate medical history PDF",
                    e
            );
        }
    }

    private UUID requireCurrentHospital() {

        UUID hospitalId =
                tenantContextService
                        .getCurrentUserHospitalId();

        if (hospitalId == null) {

            throw new AccessDeniedException(
                    "Tenant hospital context is required"
            );
        }

        return hospitalId;
    }

    private UserEntity requireCurrentUser() {

        UserEntity currentUser =
                SecurityUtils.getCurrentUser();

        if (currentUser == null) {

            throw new AccessDeniedException(
                    "Unauthenticated user"
            );
        }

        return currentUser;
    }

    private String getRole(UserEntity user) {

        if (user.getRole() == null
                || user.getRole().getRoleName() == null
                || user.getRole()
                .getRoleName()
                .isBlank()) {

            throw new AccessDeniedException(
                    "User role is not available"
            );
        }

        return user.getRole()
                .getRoleName()
                .trim()
                .toUpperCase(Locale.ROOT);
    }

    private void validatePatientHospital(
            PatientEntity patient,
            UUID hospitalId) {

        if (patient.getHospital() == null
                || patient.getHospital().getId() == null) {

            throw new AccessDeniedException(
                    "Patient hospital context is missing"
            );
        }

        if (!hospitalId.equals(
                patient.getHospital().getId())) {

            throw new AccessDeniedException(
                    "You are not authorized to access this patient's medical history"
            );
        }
    }

    private void validatePatientAccess(
            PatientEntity patient,
            UserEntity currentUser,
            String role) {

        if ("PATIENT".equals(role)) {

            if (patient.getAccount() == null
                    || patient.getAccount().getId() == null
                    || !currentUser.getId().equals(
                    patient.getAccount().getId())) {

                throw new AccessDeniedException(
                        "You are not authorized to view this patient's medical history"
                );
            }

            return;
        }

        if (!"ADMIN".equals(role)
                && !"DOCTOR".equals(role)) {

            throw new AccessDeniedException(
                    "You are not authorized to view medical history"
            );
        }
    }

    private DoctorEntity findCurrentDoctor(
            UserEntity currentUser,
            UUID hospitalId) {

        return doctorRepo
                .findByAccountId(currentUser.getId())
                .filter(doctor ->
                        doctor.getHospital() != null
                                && hospitalId.equals(
                                doctor.getHospital().getId()
                        ))
                .orElseThrow(() ->
                        new AccessDeniedException(
                                "Doctor profile not found"
                        ));
    }

    private ConsultationResponse mapConsultation(
            ConsultationEntity entity) {

        return ConsultationResponse.builder()
                .id(entity.getId())
                .appointmentId(
                        entity.getAppointment() != null
                                ? entity.getAppointment().getId()
                                : null
                )
                .doctorId(
                        entity.getDoctor() != null
                                ? entity.getDoctor().getId()
                                : null
                )
                .doctorName(
                        getDoctorName(
                                entity.getDoctor()
                        )
                )
                .patientId(
                        entity.getPatient() != null
                                ? entity.getPatient().getId()
                                : null
                )
                .patientName(
                        getPatientName(
                                entity.getPatient()
                        )
                )
                .status(
                        entity.getStatus() != null
                                ? entity.getStatus().name()
                                : null
                )
                .startTime(
                        entity.getStartTime()
                )
                .endTime(
                        entity.getEndTime()
                )
                .remarks(
                        entity.getRemarks()
                )
                .visitSaved(
                        entity.isVisitSaved()
                )
                .createdAt(
                        entity.getCreatedAt()
                )
                .updatedAt(
                        entity.getUpdatedAt()
                )
                .build();
    }

    private String getDoctorName(
            DoctorEntity doctor) {

        if (doctor == null
                || doctor.getAccount() == null) {

            return "";
        }

        String first =
                safe(
                        doctor.getAccount()
                                .getFirstName()
                );

        String last =
                safe(
                        doctor.getAccount()
                                .getLastName()
                );

        return (first + " " + last).trim();
    }

    private String getPatientName(
            PatientEntity patient) {

        if (patient == null
                || patient.getAccount() == null) {

            return "";
        }

        String first =
                safe(
                        patient.getAccount()
                                .getFirstName()
                );

        String last =
                safe(
                        patient.getAccount()
                                .getLastName()
                );

        return (first + " " + last).trim();
    }

    private String safe(Object value) {

        return value == null
                ? ""
                : String.valueOf(value);
    }
}
