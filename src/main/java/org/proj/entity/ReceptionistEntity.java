package org.proj.entity;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "receptionist", uniqueConstraints = @UniqueConstraint(columnNames = { "employeeCode", "hospital_id" }))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReceptionistEntity {

    public enum Shift {
        MORNING,
        AFTERNOON,
        NIGHT
    }

    public enum ReceptionistStatus {
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private DepartmentEntity department;

    @Column(nullable = false, length = 50)
    private String employeeCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Shift shift;

    @Column(nullable = false)
    private LocalDate joiningDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReceptionistStatus status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = ReceptionistStatus.ACTIVE;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
