package org.proj.entity;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "account")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, unique = true, updatable = false, length = 36)
    private UUID userId;

    private String firstName;
    private String middleName;
    private String lastName;

    @Column(nullable = false, unique = true)
    private String email;

    private String password;

    @Column(nullable = false)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    private Role role;

    @Builder.Default
    private Boolean isActive = true;

    @Builder.Default
    private Boolean isDeleted = false;

    @Column
    private LocalDateTime lastLogin;

    @PrePersist

    public void generateUserId() {
        if (userId == null) {
            userId = UUID.randomUUID();
        }
    }

    public enum Role {
        ADMIN,
        DOCTOR,
        PATIENT,
        RECEPTIONIST
    }
}
