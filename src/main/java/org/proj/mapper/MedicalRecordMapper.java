package org.proj.mapper;

import org.proj.dto.MedicalRecordRequest;
import org.proj.dto.MedicalRecordResponse;
import org.proj.entity.MedicalRecordEntity;
import org.springframework.stereotype.Component;

@Component
public class MedicalRecordMapper {

    public MedicalRecordEntity toEntity(MedicalRecordRequest request) {
        if (request == null) {
            return null;
        }

        return MedicalRecordEntity.builder()
                .diagnosis(request.getDiagnosis())
                .notes(request.getNotes())
                .recordDate(request.getRecordDate())
                .build();
    }

    public MedicalRecordResponse toResponse(MedicalRecordEntity entity) {
        if (entity == null) {
            return null;
        }

        String doctorName = "";
        String doctorSpecialization = "";
        if (entity.getDoctor() != null) {
            if (entity.getDoctor().getAccount() != null) {
                String first = entity.getDoctor().getAccount().getFirstName() != null ? entity.getDoctor().getAccount().getFirstName() : "";
                String last = entity.getDoctor().getAccount().getLastName() != null ? entity.getDoctor().getAccount().getLastName() : "";
                doctorName = (first + " " + last).trim();
            }
            doctorSpecialization = entity.getDoctor().getSpecialization();
        }

        String patientName = "";
        String patientCode = "";
        if (entity.getPatient() != null) {
            patientCode = entity.getPatient().getPatientCode();
            if (entity.getPatient().getAccount() != null) {
                String first = entity.getPatient().getAccount().getFirstName() != null ? entity.getPatient().getAccount().getFirstName() : "";
                String last = entity.getPatient().getAccount().getLastName() != null ? entity.getPatient().getAccount().getLastName() : "";
                patientName = (first + " " + last).trim();
            }
        }

        return MedicalRecordResponse.builder()
                .id(entity.getId())
                .patientId(entity.getPatient() != null ? entity.getPatient().getId() : null)
                .patientName(patientName)
                .patientCode(patientCode)
                .doctorId(entity.getDoctor() != null ? entity.getDoctor().getId() : null)
                .doctorName(doctorName)
                .doctorSpecialization(doctorSpecialization)
                .appointmentId(entity.getAppointment() != null ? entity.getAppointment().getId() : null)
                .appointmentNumber(entity.getAppointment() != null ? entity.getAppointment().getAppointmentNumber() : null)
                .diagnosis(entity.getDiagnosis())
                .notes(entity.getNotes())
                .recordDate(entity.getRecordDate())
                .createdAt(entity.getCreatedAt())
                .message("Success")
                .build();
    }
}
