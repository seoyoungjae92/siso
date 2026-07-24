package com.siso.backend.admin;

import com.siso.backend.settings.ModerationSettings;
import com.siso.backend.settings.ModerationSettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
public class AdminModerationSettingsService {

    private static final short SINGLETON_ID = 1;

    private final ModerationSettingsRepository moderationSettingsRepository;

    public AdminModerationSettingsService(ModerationSettingsRepository moderationSettingsRepository) {
        this.moderationSettingsRepository = moderationSettingsRepository;
    }

    @Transactional(readOnly = true)
    public ModerationSettingsDto get() {
        return toDto(findSingleton());
    }

    @Transactional
    public ModerationSettingsDto update(ModerationSettingsRequest request) {
        ModerationSettings settings = findSingleton();
        settings.update(request.autoBlindReportThreshold(), OffsetDateTime.now());
        return toDto(settings);
    }

    private ModerationSettings findSingleton() {
        return moderationSettingsRepository.findById(SINGLETON_ID).orElseThrow();
    }

    private ModerationSettingsDto toDto(ModerationSettings settings) {
        return new ModerationSettingsDto(settings.getAutoBlindReportThreshold(), settings.getUpdatedAt());
    }
}
