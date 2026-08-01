package com.siso.backend.admin;

import com.siso.backend.settings.AbuseSettings;
import com.siso.backend.settings.AbuseSettingsRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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
        // TrustScoreService가 ageHours / trustMaturityHours로 나눠서 성숙도를
        // 계산한다 — 0 이하가 들어오면 0으로 나누기(NaN)가 그대로 DB에
        // 영구 저장돼 투표 화면에 "NaN%"까지 노출되는 걸 실제로 겪음.
        if (request.trustMaturityHours() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "trustMaturityHours는 1 이상이어야 합니다");
        }

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
