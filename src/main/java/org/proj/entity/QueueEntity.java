package org.proj.entity;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "queue", uniqueConstraints = @UniqueConstraint(columnNames = { "appointment_id" }))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueueEntity {

    public enum QueueStatus {
        WAITING,
        CALLED,
        IN_CONSULTATION,
        COMPLETED,
        SKIPPED,
        CANCELLED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false, length = 36)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID id;

    @Column(nullable = false)
    private Integer queueNumber;

    @Column(nullable = false, length = 20)
    private String tokenNumber;

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

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_id", nullable = false, unique = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private AppointmentEntity appointment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QueueStatus queueStatus;

    @Column(nullable = false)
    private LocalDateTime checkedInTime;

    @Column
    private LocalDateTime consultationStart;

    @Column
    private LocalDateTime consultationEnd;

    @Column
    private LocalDateTime queueReminderSentAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.queueStatus == null) {
            this.queueStatus = QueueStatus.WAITING;
        }
        if (this.checkedInTime == null) {
            this.checkedInTime = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
