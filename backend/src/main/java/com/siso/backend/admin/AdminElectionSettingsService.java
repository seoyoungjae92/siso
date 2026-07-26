package com.siso.backend.admin;

import com.siso.backend.settings.ElectionSettings;
import com.siso.backend.settings.ElectionSettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
public class AdminElectionSettingsService {

    private static final short SINGLETON_ID = 1;

    private final ElectionSettingsRepository electionSettingsRepository;

    public AdminElectionSettingsService(ElectionSettingsRepository electionSettingsRepository) {
        this.electionSettingsRepository = electionSettingsRepository;
    }

    @Transactional(readOnly = true)
    public ElectionSettingsDto get() {
        return toDto(findSingleton());
    }

    @Transactional
    public ElectionSettingsDto update(ElectionSettingsRequest request) {
        ElectionSettings settings = findSingleton();
        settings.update(request.enabled(), request.overrideAutoBlindThreshold(), OffsetDateTime.now());
        return toDto(settings);
    }

    private ElectionSettings findSingleton() {
        return electionSettingsRepository.findById(SINGLETON_ID).orElseThrow();
    }

    private ElectionSettingsDto toDto(ElectionSettings settings) {
        return new ElectionSettingsDto(
                settings.isEnabled(), settings.getOverrideAutoBlindThreshold(), settings.getUpdatedAt());
    }
}
