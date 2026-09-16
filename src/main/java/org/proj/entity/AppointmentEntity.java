package org.proj.entity;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "appointment", uniqueConstraints = @UniqueConstraint(columnNames = { "appointmentNumber",
        "hospital_id" }))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentEntity {

    public enum AppointmentType {
        WALK_IN,
        ONLINE,
        FOLLOW_UP
    }

    public enum AppointmentPriority {
        NORMAL,
        URGENT,
        EMERGENCY
    }

    public enum AppointmentStatus {
        SCHEDULED,
        CHECKED_IN,
        IN_PROGRESS,
        COMPLETED,
        CANCELLED,
        NO_SHOW
    }

    public enum ConsultationMode {
        OPD,
        VIDEO,
        HOME_VISIT
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false, length = 36)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID id;

    @Column(nullable = false, unique = true, length = 30)
    private String appointmentNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hospital_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private HospitalEntity hospital;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private DepartmentEntity department;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private DoctorEntity doctor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private PatientEntity patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receptionist_id", nullable = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private ReceptionistEntity bookedByReceptionist;

    @OneToOne(mappedBy = "appointment", fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private QueueEntity queue;

    @Column(nullable = false)
    private LocalDate appointmentDate;

    @Column(nullable = false)
    private LocalTime appointmentTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AppointmentPriority priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AppointmentType appointmentType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AppointmentStatus appointmentStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConsultationMode consultationMode;

    @Column(length = 500)
    private String remarks;

    /**
     * Timestamp of the appointment reminder notification.
     * Null means the reminder has not been sent yet.
     */
    @Column
    private LocalDateTime appointmentReminderSentAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.appointmentStatus == null) {
            this.appointmentStatus = AppointmentStatus.SCHEDULED;
        }
        if (this.priority == null) {
            this.priority = AppointmentPriority.NORMAL;
        }

        if (this.appointmentType == null) {
            this.appointmentType = AppointmentType.WALK_IN;
        }

        if (this.consultationMode == null) {
            this.consultationMode = ConsultationMode.OPD;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
