package org.proj.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.proj.entity.AppointmentEntity.AppointmentType;
import org.proj.entity.AppointmentEntity.AppointmentStatus;
import org.proj.entity.AppointmentEntity.ConsultationMode;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentRequest {

    @NotNull(message = "Hospital ID is required")
    private UUID hospitalId;

    @NotNull(message = "Department ID is required")
    private UUID departmentId;

    @NotNull(message = "Doctor ID is required")
    private UUID doctorId;

    @NotNull(message = "Patient ID is required")
    private UUID patientId;

    private UUID bookedByReceptionistId;

    @NotNull(message = "Appointment date is required")
    @FutureOrPresent(message = "Appointment date must be in the present or future")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate appointmentDate;

    @NotNull(message = "Appointment time is required")
    @JsonFormat(pattern = "HH:mm")
    private LocalTime appointmentTime;

    @NotNull(message = "Appointment type is required")
    private AppointmentType appointmentType;

    @Builder.Default
    private AppointmentStatus appointmentStatus = AppointmentStatus.SCHEDULED;

    @NotNull(message = "Consultation mode is required")
    private ConsultationMode consultationMode;

    private String remarks;
}
