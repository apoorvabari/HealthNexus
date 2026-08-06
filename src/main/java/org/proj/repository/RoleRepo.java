package org.proj.repository;

import org.proj.entity.RoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoleRepo extends JpaRepository<RoleEntity, UUID> {
    Optional<RoleEntity> findByRoleName(String roleName);
}
