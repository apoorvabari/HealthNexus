package org.proj.entity;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import jakarta.persistence.*;
import lombok.*;
import jakarta.validation.constraints.Email;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "hospital")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HospitalEntity {

    public enum HospitalType {
        GOVERNMENT,
        PRIVATE,
        TRUST,
        CLINIC
    }

    public enum HospitalStatus {
        ACTIVE,
        INACTIVE,
        SUSPENDED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false, length = 36)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID id;

    @Column(nullable = false, unique = true, length = 50)
    private String hospitalCode;
    @Column(nullable = false)
    private String hospitalName;

    @Email(message = "Invalid email address")
    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String phoneNumber;

    @Column(nullable = false)
    private String address;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private String state;

    @Column(nullable = false)
    private String postalCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private HospitalType hospitalType;

    @Column(nullable = false, unique = true)
    private String registrationNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private HospitalStatus status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = HospitalStatus.ACTIVE;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
