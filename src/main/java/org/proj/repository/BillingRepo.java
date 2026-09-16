package org.proj.repository;

import org.proj.entity.BillingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BillingRepo extends JpaRepository<BillingEntity, UUID> {

    Optional<BillingEntity> findByIdAndAppointmentHospitalId(UUID id, UUID hospitalId);

    Optional<BillingEntity> findByAppointmentIdAndAppointmentHospitalId(UUID appointmentId, UUID hospitalId);

    boolean existsById(UUID id);

    boolean existsByAppointmentId(UUID appointmentId);

    Optional<BillingEntity> findByInvoiceNumber(String invoiceNumber);

    List<BillingEntity> findByPatientIdAndAppointmentHospitalIdOrderByBillingDateDesc(UUID patientId, UUID hospitalId);

    @Query("SELECT b FROM BillingEntity b WHERE b.patient.id = :patientId AND b.appointment.doctor.account.id = :doctorAccountId AND b.appointment.hospital.id = :hospitalId ORDER BY b.billingDate DESC")
    List<BillingEntity> findByPatientIdAndDoctorAccountIdAndHospitalId(
            @Param("patientId") UUID patientId,
            @Param("doctorAccountId") UUID doctorAccountId,
            @Param("hospitalId") UUID hospitalId
    );
}
