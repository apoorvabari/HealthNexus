package org.proj.dto;

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
public class AppointmentResponse {
    private UUID id;
    private String appointmentNumber;
    private UUID hospitalId;
    private String hospitalName;
    private UUID departmentId;
    private String departmentName;
    private UUID doctorId;
    private String doctorName;
    private UUID patientId;
    private String patientName;
    private UUID bookedByReceptionistId;
    private String bookedByReceptionistName;
    private LocalDate appointmentDate;
    private LocalTime appointmentTime;
    private AppointmentType appointmentType;
    private AppointmentStatus appointmentStatus;
    private ConsultationMode consultationMode;
    private String remarks;
    private String message;
}
