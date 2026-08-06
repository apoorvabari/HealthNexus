package org.proj.entity;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.*;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserEntity implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false, length = 36)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID id;

    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, unique = true, updatable = false, length = 36)
    private UUID userId;

    private String firstName;
    private String middleName;
    private String lastName;

    @Email
    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String phoneNumber;

    @Column
    private String password;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id", nullable = false)
    private RoleEntity role;

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

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.getRoleName()));
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return isActive && !isDeleted;
    }
}
