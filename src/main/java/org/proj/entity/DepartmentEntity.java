package org.proj.entity;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "department", uniqueConstraints = @UniqueConstraint(columnNames = { "departmentCode", "hospital_id" }))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentEntity {

    public enum DepartmentStatus {
        ACTIVE,
        INACTIVE
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false, length = 36)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID id;

    @Column(nullable = false, length = 50)
    private String departmentCode;

    @Column(nullable = false)
    private String departmentName;

    @Column
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DepartmentStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hospital_id", nullable = false)
    private HospitalEntity hospital;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = DepartmentStatus.ACTIVE;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
