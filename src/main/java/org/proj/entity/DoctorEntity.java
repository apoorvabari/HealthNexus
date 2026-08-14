package org.proj.entity;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "doctor")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoctorEntity {

    public enum DoctorStatus {
        ACTIVE,
        INACTIVE,
        SUSPENDED
    }

    public enum VerificationStatus {
        PENDING,
        APPROVED,
        REJECTED
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private DepartmentEntity department;

    @Column(nullable = false)
    private String specialization;

    @Column(nullable = false)
    private String qualification;

    @Column(nullable = false)
    private Integer experience;

    @Column(nullable = false)
    private BigDecimal consultationFee;

    @Column(nullable = false, unique = true)
    private String licenseNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private DoctorStatus status = DoctorStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status")
    @Builder.Default
    private VerificationStatus verificationStatus = VerificationStatus.PENDING;

     @Column(name = "license_verified")
    @Builder.Default
    private Boolean licenseVerified = false;

    @Column(name = "degree_verified")
    @Builder.Default
    private Boolean degreeVerified = false;

    @Column(name = "specialization_verified")
    @Builder.Default
    private Boolean specializationVerified = false;

    @Column(name = "verification_remarks", length = 500)
    private String verificationRemarks;

    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "verified_by", length = 36)
    private UUID verifiedBy;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = DoctorStatus.ACTIVE;
        }

        if (this.verificationStatus == null) {
            this.verificationStatus = VerificationStatus.PENDING;
        }

        if (this.licenseVerified == null) {
            this.licenseVerified = false;
        }

        if (this.degreeVerified == null) {
            this.degreeVerified = false;
        }

        if (this.specializationVerified == null) {
            this.specializationVerified = false;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
