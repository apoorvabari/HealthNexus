package org.proj.service;

import org.proj.entity.AdminSystemSettingsEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AdminSystemSettingsService {

    Optional<AdminSystemSettingsEntity> findByHospitalIdAndSettingKey(UUID hospitalId, String settingKey);

    List<AdminSystemSettingsEntity> findAllByHospitalId(UUID hospitalId);

    AdminSystemSettingsEntity save(AdminSystemSettingsEntity setting);
}
