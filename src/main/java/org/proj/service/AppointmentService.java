package org.proj.service;

import org.proj.dto.AppointmentRequest;
import org.proj.dto.AppointmentResponse;
import org.proj.entity.AppointmentEntity;

import java.util.List;
import java.util.UUID;

public interface AppointmentService {

    AppointmentResponse createAppointment(AppointmentRequest request);

    AppointmentResponse getAppointmentById(UUID id);

    List<AppointmentResponse> getAllAppointments();

    AppointmentResponse updateAppointment(UUID id, AppointmentRequest request);

    void deleteAppointment(UUID id);

    List<AppointmentResponse> getTodayAppointments();

    List<AppointmentResponse> getAppointmentsByDoctor(UUID doctorId);

    List<AppointmentResponse> getAppointmentsByPatient(UUID patientId);

    AppointmentEntity findAppointmentById(UUID appointmentId);

    void save(AppointmentEntity app);

    long count();

    long countByAppointmentDate(java.time.LocalDate date);
}
