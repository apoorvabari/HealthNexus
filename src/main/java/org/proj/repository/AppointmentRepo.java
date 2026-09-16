package org.proj.repository;

import jakarta.persistence.LockModeType;
import org.proj.entity.AppointmentEntity;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AppointmentRepo extends JpaRepository<AppointmentEntity, UUID> {

    boolean existsByAppointmentNumber(String appointmentNumber);

    boolean existsByDoctorIdAndAppointmentDateAndAppointmentTime(
            UUID doctorId,
            LocalDate date,
            LocalTime time
    );

    boolean existsByDoctorIdAndAppointmentDateAndAppointmentTimeAndIdNot(
            UUID doctorId,
            LocalDate date,
            LocalTime time,
            UUID id
    );

    /*
     * Hospital-scoped availability checks
     */
    boolean existsByDoctorIdAndAppointmentDateAndAppointmentTimeAndHospitalId(
            UUID doctorId,
            LocalDate date,
            LocalTime time,
            UUID hospitalId
    );

    boolean existsByDoctorIdAndAppointmentDateAndAppointmentTimeAndIdNotAndHospitalId(
            UUID doctorId,
            LocalDate date,
            LocalTime time,
            UUID id,
            UUID hospitalId
    );

    List<AppointmentEntity> findByAppointmentDate(LocalDate date);

    List<AppointmentEntity> findByAppointmentDateAndHospitalId(
            LocalDate date,
            UUID hospitalId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<AppointmentEntity> findByAppointmentDateAndAppointmentStatusAndAppointmentReminderSentAtIsNull(
            LocalDate date,
            AppointmentEntity.AppointmentStatus status
    );

    List<AppointmentEntity> findByDoctorId(UUID doctorId);

    List<AppointmentEntity> findByDoctorIdAndHospitalId(
            UUID doctorId,
            UUID hospitalId
    );

    List<AppointmentEntity> findByDoctorIdAndAppointmentDate(
            UUID doctorId,
            LocalDate date
    );

    /*
     * Hospital-scoped doctor/date lookup
     */
    List<AppointmentEntity> findByDoctorIdAndAppointmentDateAndHospitalId(
            UUID doctorId,
            LocalDate date,
            UUID hospitalId
    );

    List<AppointmentEntity> findByPatientId(UUID patientId);

    List<AppointmentEntity> findByPatientIdAndHospitalId(
            UUID patientId,
            UUID hospitalId
    );

    List<AppointmentEntity> findByHospitalId(UUID hospitalId);

    Optional<AppointmentEntity> findByIdAndHospitalId(
            UUID id,
            UUID hospitalId
    );

    boolean existsByDoctorIdAndPatientId(
            UUID doctorId,
            UUID patientId
    );

    boolean existsByDoctorIdAndPatientIdAndHospitalId(
            UUID doctorId,
            UUID patientId,
            UUID hospitalId
    );

    long countByAppointmentDate(LocalDate date);

    long countByHospitalId(UUID hospitalId);

    long countByAppointmentDateAndHospitalId(
            LocalDate date,
            UUID hospitalId
    );
}