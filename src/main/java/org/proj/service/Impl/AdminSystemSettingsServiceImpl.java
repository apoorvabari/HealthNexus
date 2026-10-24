package org.proj.service.impl;

import lombok.RequiredArgsConstructor;
import org.proj.entity.AdminSystemSettingsEntity;
import org.proj.repository.AdminSystemSettingsRepo;
import org.proj.service.AdminSystemSettingsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminSystemSettingsServiceImpl implements AdminSystemSettingsService {

    private final AdminSystemSettingsRepo systemSettingsRepo;

    @Override
    @Transactional(readOnly = true)
    public Optional<AdminSystemSettingsEntity> findByHospitalIdAndSettingKey(UUID hospitalId, String settingKey) {
        try {
            return (hospitalId == null || settingKey == null || settingKey.isBlank())
                    ? Optional.empty()
                    : systemSettingsRepo.findByHospitalIdAndSettingKey(hospitalId, settingKey.trim());
        } catch (Exception e) {
            String msg = e.getMessage() != null && !e.getMessage().isBlank()
                    ? e.getMessage()
                    : "Unable to find system setting";
            throw new RuntimeException(msg, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminSystemSettingsEntity> findAllByHospitalId(UUID hospitalId) {
        try {
            return hospitalId == null
                    ? List.of()
                    : systemSettingsRepo.findAllByHospitalId(hospitalId);
        } catch (Exception e) {
            String msg = e.getMessage() != null && !e.getMessage().isBlank()
                    ? e.getMessage()
                    : "Unable to find system settings by hospital";
            throw new RuntimeException(msg, e);
        }
    }

    @Override
    @Transactional
    public AdminSystemSettingsEntity save(AdminSystemSettingsEntity setting) {
        try {
            if (setting == null) {
                throw new IllegalArgumentException("System setting cannot be null");
            }
            return systemSettingsRepo.save(setting);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            String msg = e.getMessage() != null && !e.getMessage().isBlank()
                    ? e.getMessage()
                    : "Unable to save system setting";
            throw new RuntimeException(msg, e);
        }
    }

}
