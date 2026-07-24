package com.siso.backend.admin;

import com.siso.backend.settings.CrawlSettings;
import com.siso.backend.settings.CrawlSettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
public class AdminCrawlSettingsService {

    private static final short SINGLETON_ID = 1;

    private final CrawlSettingsRepository crawlSettingsRepository;

    public AdminCrawlSettingsService(CrawlSettingsRepository crawlSettingsRepository) {
        this.crawlSettingsRepository = crawlSettingsRepository;
    }

    @Transactional(readOnly = true)
    public CrawlSettingsDto get() {
        return toDto(findSingleton());
    }

    @Transactional
    public CrawlSettingsDto update(CrawlSettingsRequest request) {
        CrawlSettings settings = findSingleton();
        settings.update(
                request.matchSimilarityThreshold(),
                request.pruneSimilarityThreshold(),
                request.minClusterSize(),
                request.gracePeriodHours(),
                request.displayWindowDays(),
                request.synthesisLimit(),
                request.synthesisModel(),
                OffsetDateTime.now());
        return toDto(settings);
    }

    private CrawlSettings findSingleton() {
        return crawlSettingsRepository.findById(SINGLETON_ID).orElseThrow();
    }

    private CrawlSettingsDto toDto(CrawlSettings settings) {
        return new CrawlSettingsDto(
                settings.getMatchSimilarityThreshold(),
                settings.getPruneSimilarityThreshold(),
                settings.getMinClusterSize(),
                settings.getGracePeriodHours(),
                settings.getDisplayWindowDays(),
                settings.getSynthesisLimit(),
                settings.getSynthesisModel(),
                settings.getUpdatedAt());
    }
}
