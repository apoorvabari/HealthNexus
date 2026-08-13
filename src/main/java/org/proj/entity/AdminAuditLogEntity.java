package org.proj.entity;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "admin_audit_log")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminAuditLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false, length = 36)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID id;

    @Column(nullable = false)
    private String action;

    @Column(nullable = false)
    private String entityName;

    @Column(nullable = false, length = 36)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID entityId;

    @Column(nullable = false, length = 36)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID performedBy;

    @Lob
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    private String details;

    @Column(nullable = false, updatable = false)
    private LocalDateTime timestamp;

    @PrePersist
    protected void onCreate() {
        this.timestamp = LocalDateTime.now();
    }
}
