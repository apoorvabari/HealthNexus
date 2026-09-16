package org.proj.repository;

import org.proj.entity.PasswordResetTokenEntity;
import org.proj.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PasswordResetTokenRepo
        extends JpaRepository<PasswordResetTokenEntity, UUID> {

    Optional<PasswordResetTokenEntity>
    findByTokenAndUsedFalse(String token);

    List<PasswordResetTokenEntity>
    findAllByUserAndUsedFalse(UserEntity user);
}
