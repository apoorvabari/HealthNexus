package org.proj.repository;

import org.proj.entity.AdminEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AdminRepo extends JpaRepository<AdminEntity, UUID> {

    /*
     * All hospital assignments belonging to an ADMIN.
     */
    List<AdminEntity> findAllByAccountId(UUID accountId);

    /*
     * Used only where existing code needs one
     * deterministic ADMIN assignment.
     */
    Optional<AdminEntity> findFirstByAccountIdOrderByCreatedAtAsc(
            UUID accountId
    );

    /*
     * Exact ADMIN -> HOSPITAL ownership check.
     */
    Optional<AdminEntity> findByAccountIdAndHospitalId(
            UUID accountId,
            UUID hospitalId
    );

    boolean existsByAccountId(UUID accountId);

    /*
     * Prevent duplicate assignment of the same
     * ADMIN to the same hospital.
     */
    boolean existsByAccountIdAndHospitalId(
            UUID accountId,
            UUID hospitalId
    );
}