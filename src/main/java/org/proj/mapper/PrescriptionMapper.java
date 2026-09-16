package org.proj.mapper;

import org.proj.dto.PrescriptionMedicineRequest;
import org.proj.dto.PrescriptionMedicineResponse;
import org.proj.dto.PrescriptionRequest;
import org.proj.dto.PrescriptionResponse;
import org.proj.entity.PrescriptionEntity;
import org.proj.entity.PrescriptionMedicineEntity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class PrescriptionMapper {

    public PrescriptionEntity toEntity(PrescriptionRequest request) {
        if (request == null) {
            return null;
        }

        PrescriptionEntity prescription = PrescriptionEntity.builder()
                .instructions(request.getInstructions())
                .medicines(new ArrayList<>())
                .build();

        if (request.getMedicines() != null) {
            for (PrescriptionMedicineRequest medRequest : request.getMedicines()) {
                PrescriptionMedicineEntity medEntity = PrescriptionMedicineEntity.builder()
                        .medicineName(medRequest.getMedicineName())
                        .dosage(medRequest.getDosage())
                        .frequency(medRequest.getFrequency())
                        .duration(medRequest.getDuration())
                        .instructions(medRequest.getInstructions())
                        .prescription(prescription)
                        .build();
                prescription.getMedicines().add(medEntity);
            }
        }

        return prescription;
    }

    public PrescriptionResponse toResponse(PrescriptionEntity entity) {
        if (entity == null) {
            return null;
        }

        List<PrescriptionMedicineResponse> medicineResponses = new ArrayList<>();
        if (entity.getMedicines() != null) {
            medicineResponses = entity.getMedicines().stream()
                    .map(this::toMedicineResponse)
                    .collect(Collectors.toList());
        }

        return PrescriptionResponse.builder()
                .id(entity.getId())
                .patientId(entity.getPatient() != null ? entity.getPatient().getId() : null)
                .patientName(entity.getPatient() != null && entity.getPatient().getAccount() != null ? 
                        entity.getPatient().getAccount().getFirstName() + " " + entity.getPatient().getAccount().getLastName() : null)
                .doctorId(entity.getDoctor() != null ? entity.getDoctor().getId() : null)
                .doctorName(entity.getDoctor() != null && entity.getDoctor().getAccount() != null ? 
                        entity.getDoctor().getAccount().getFirstName() + " " + entity.getDoctor().getAccount().getLastName() : null)
                .appointmentId(entity.getAppointment() != null ? entity.getAppointment().getId() : null)
                .medicalRecordId(entity.getMedicalRecord() != null ? entity.getMedicalRecord().getId() : null)
                .prescriptionDate(entity.getPrescriptionDate())
                .instructions(entity.getInstructions())
                .medicines(medicineResponses)
                .build();
    }

    private PrescriptionMedicineResponse toMedicineResponse(PrescriptionMedicineEntity entity) {
        if (entity == null) {
            return null;
        }
        return PrescriptionMedicineResponse.builder()
                .id(entity.getId())
                .medicineName(entity.getMedicineName())
                .dosage(entity.getDosage())
                .frequency(entity.getFrequency())
                .duration(entity.getDuration())
                .instructions(entity.getInstructions())
                .build();
    }
}
