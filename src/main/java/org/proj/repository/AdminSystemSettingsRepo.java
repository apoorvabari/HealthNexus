package org.proj.repository;

import org.proj.entity.AdminSystemSettingsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AdminSystemSettingsRepo extends JpaRepository<AdminSystemSettingsEntity, UUID> {
    Optional<AdminSystemSettingsEntity> findBySettingKey(String settingKey);
}
