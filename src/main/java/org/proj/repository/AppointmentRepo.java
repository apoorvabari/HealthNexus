package org.proj.repository;

import org.proj.entity.AppointmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface AppointmentRepo extends JpaRepository<AppointmentEntity, UUID> {

    boolean existsByAppointmentNumber(String appointmentNumber);

    boolean existsByDoctorIdAndAppointmentDateAndAppointmentTime(UUID doctorId, LocalDate date, LocalTime time);

    boolean existsByDoctorIdAndAppointmentDateAndAppointmentTimeAndIdNot(UUID doctorId, LocalDate date, LocalTime time, UUID id);

    List<AppointmentEntity> findByAppointmentDate(LocalDate date);

    List<AppointmentEntity> findByDoctorId(UUID doctorId);

    List<AppointmentEntity> findByPatientId(UUID patientId);
}
