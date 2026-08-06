package org.proj.entity;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "patient", uniqueConstraints = @UniqueConstraint(columnNames = { "patientCode", "hospital_id" }))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientEntity {

    public enum Gender {
        MALE,
        FEMALE,
        OTHER
    }

    public enum BloodGroup {
        A_POSITIVE,
        A_NEGATIVE,
        B_POSITIVE,
        B_NEGATIVE,
        AB_POSITIVE,
        AB_NEGATIVE,
        O_POSITIVE,
        O_NEGATIVE
    }

    public enum PatientStatus {
        ACTIVE,
        INACTIVE
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false, length = 36)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false, unique = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private UserEntity account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hospital_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private HospitalEntity hospital;

    @Column(nullable = false, length = 50)
    private String patientCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BloodGroup bloodGroup;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Gender gender;

    @Column(nullable = false)
    private LocalDate dateOfBirth;

    @Column(nullable = false)
    private String emergencyContactName;

    @Column(nullable = false)
    private String emergencyContactPhone;

    @Column(nullable = false)
    private String relationship;

    @Column(nullable = false)
    private String address;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private String state;

    @Column(nullable = false, length = 10)
    private String postalCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PatientStatus status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = PatientStatus.ACTIVE;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
