package com.siso.backend.admin;

import com.siso.backend.settings.AbuseSettings;
import com.siso.backend.settings.AbuseSettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
public class AdminAbuseSettingsService {

    private static final short SINGLETON_ID = 1;

    private final AbuseSettingsRepository abuseSettingsRepository;

    public AdminAbuseSettingsService(AbuseSettingsRepository abuseSettingsRepository) {
        this.abuseSettingsRepository = abuseSettingsRepository;
    }

    @Transactional(readOnly = true)
    public AbuseSettingsDto get() {
        return toDto(findSingleton());
    }

    @Transactional
    public AbuseSettingsDto update(AbuseSettingsRequest request) {
        AbuseSettings settings = findSingleton();
        settings.update(
                request.multiAccountClusterSize(),
                request.multiAccountTrustPenaltyMultiplier(),
                request.trustMaturityHours(),
                request.trustMinWeight(),
                request.duplicateSimilarityThreshold(),
                request.duplicateLookbackCount(),
                request.duplicateLookbackMinutes(),
                request.spikeWindowMinutes(),
                request.spikeVoteThreshold(),
                request.spikeReactionThreshold(),
                OffsetDateTime.now());
        return toDto(settings);
    }

    private AbuseSettings findSingleton() {
        return abuseSettingsRepository.findById(SINGLETON_ID).orElseThrow();
    }

    private AbuseSettingsDto toDto(AbuseSettings settings) {
        return new AbuseSettingsDto(
                settings.getMultiAccountClusterSize(),
                settings.getMultiAccountTrustPenaltyMultiplier(),
                settings.getTrustMaturityHours(),
                settings.getTrustMinWeight(),
                settings.getDuplicateSimilarityThreshold(),
                settings.getDuplicateLookbackCount(),
                settings.getDuplicateLookbackMinutes(),
                settings.getSpikeWindowMinutes(),
                settings.getSpikeVoteThreshold(),
                settings.getSpikeReactionThreshold(),
                settings.getUpdatedAt());
    }
}
