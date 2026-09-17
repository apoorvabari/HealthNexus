package org.proj.repository;

import org.proj.entity.AdminEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AdminRepo extends JpaRepository<AdminEntity, UUID> {

    List<AdminEntity> findAllByAccountId(UUID accountId);

    Optional<AdminEntity> findFirstByAccountIdOrderByCreatedAtAsc(
            UUID accountId
    );

    Optional<AdminEntity> findByAccountIdAndHospitalId(
            UUID accountId,
            UUID hospitalId
    );

    boolean existsByAccountId(UUID accountId);

    boolean existsByAccountIdAndHospitalId(
            UUID accountId,
            UUID hospitalId
    );
}
