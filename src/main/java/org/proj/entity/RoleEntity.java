package org.proj.entity;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;
import java.util.List;
import java.util.ArrayList;

@Entity
@Table(name = "role")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "role_id", updatable = false, nullable = false, length = 36)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String roleName;

    @Builder.Default
    private Boolean isActive = true;

    @OneToMany(mappedBy = "role")
    @Builder.Default
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<UserEntity> users = new ArrayList<>();
}
