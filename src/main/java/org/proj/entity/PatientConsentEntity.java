package org.proj.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "patient_consent", uniqueConstraints = @UniqueConstraint(columnNames = {"patient_id", "consent_type"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientConsentEntity {

    public enum ConsentType {
        APPOINTMENT_BOOKING,
        MEDICAL_RECORD_SHARING,
        PRESCRIPTION_ACCESS
    }

    public enum ConsentStatus {
        GRANTED,
        DECLINED,
        REVOKED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false, length = 36)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private PatientEntity patient;

    @Enumerated(EnumType.STRING)
    @Column(name = "consent_type", nullable = false, length = 50)
    private ConsentType consentType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ConsentStatus status;

    @Column(nullable = false)
    private LocalDateTime consentedAt;

    @Column
    private LocalDateTime revokedAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.consentedAt == null) {
            this.consentedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
