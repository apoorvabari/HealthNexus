package org.proj.repository;

import org.proj.entity.EmailVerificationTokenEntity;
import org.proj.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmailVerificationTokenRepo extends JpaRepository<EmailVerificationTokenEntity, UUID> {

    Optional<EmailVerificationTokenEntity> findByTokenHash(String tokenHash);

    List<EmailVerificationTokenEntity> findByUser(UserEntity user);

    List<EmailVerificationTokenEntity> findByUserIdAndUsedAtIsNull(UUID userId);
}
