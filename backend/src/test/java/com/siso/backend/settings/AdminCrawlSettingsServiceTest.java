package com.siso.backend.settings;

import com.siso.backend.admin.AdminCrawlSettingsService;
import com.siso.backend.admin.CrawlSettingsDto;
import com.siso.backend.admin.CrawlSettingsRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminCrawlSettingsServiceTest {

    @Mock
    private CrawlSettingsRepository crawlSettingsRepository;

    private AdminCrawlSettingsService newService() {
        return new AdminCrawlSettingsService(crawlSettingsRepository);
    }

    private CrawlSettings defaults() {
        CrawlSettings settings = new CrawlSettings();
        ReflectionTestUtils.setField(settings, "id", (short) 1);
        ReflectionTestUtils.setField(settings, "matchSimilarityThreshold", 0.5f);
        ReflectionTestUtils.setField(settings, "pruneSimilarityThreshold", 0.5f);
        ReflectionTestUtils.setField(settings, "minClusterSize", 3);
        ReflectionTestUtils.setField(settings, "gracePeriodHours", 48);
        ReflectionTestUtils.setField(settings, "displayWindowDays", 7);
        ReflectionTestUtils.setField(settings, "synthesisLimit", 10);
        ReflectionTestUtils.setField(settings, "synthesisModel", "openrouter/free");
        ReflectionTestUtils.setField(settings, "deadLinkScanLimit", 100);
        ReflectionTestUtils.setField(settings, "pruneScanLimit", 100);
        ReflectionTestUtils.setField(settings, "sourceFailureThreshold", 5);
        ReflectionTestUtils.setField(settings, "cohortSimilarityThreshold", 0.5f);
        ReflectionTestUtils.setField(settings, "synthesisMinPostsPerSide", 1);
        ReflectionTestUtils.setField(settings, "detailFetchLimit", 20);
        ReflectionTestUtils.setField(settings, "postRetentionDays", 10);
        ReflectionTestUtils.setField(settings, "stalePostScanLimit", 200);
        ReflectionTestUtils.setField(settings, "updatedAt", OffsetDateTime.now());
        return settings;
    }

    @Test
    void get_returnsCurrentValues() {
        when(crawlSettingsRepository.findById((short) 1)).thenReturn(Optional.of(defaults()));

        CrawlSettingsDto dto = newService().get();

        assertThat(dto.matchSimilarityThreshold()).isEqualTo(0.5f);
        assertThat(dto.pruneSimilarityThreshold()).isEqualTo(0.5f);
        assertThat(dto.minClusterSize()).isEqualTo(3);
        assertThat(dto.gracePeriodHours()).isEqualTo(48);
        assertThat(dto.displayWindowDays()).isEqualTo(7);
        assertThat(dto.synthesisLimit()).isEqualTo(10);
        assertThat(dto.synthesisModel()).isEqualTo("openrouter/free");
        assertThat(dto.deadLinkScanLimit()).isEqualTo(100);
        assertThat(dto.pruneScanLimit()).isEqualTo(100);
        assertThat(dto.sourceFailureThreshold()).isEqualTo(5);
        assertThat(dto.cohortSimilarityThreshold()).isEqualTo(0.5f);
        assertThat(dto.synthesisMinPostsPerSide()).isEqualTo(1);
        assertThat(dto.detailFetchLimit()).isEqualTo(20);
        assertThat(dto.postRetentionDays()).isEqualTo(10);
        assertThat(dto.stalePostScanLimit()).isEqualTo(200);
    }

    @Test
    void update_overwritesAllFields() {
        CrawlSettings settings = defaults();
        when(crawlSettingsRepository.findById((short) 1)).thenReturn(Optional.of(settings));

        CrawlSettingsRequest request = new CrawlSettingsRequest(
                0.6f, 0.4f, 5, 72, 14, 20, "anthropic/claude-haiku-4.5", 50, 60, 7, 0.7f, 2, 30, 15, 300);
        CrawlSettingsDto dto = newService().update(request);

        assertThat(dto.matchSimilarityThreshold()).isEqualTo(0.6f);
        assertThat(dto.pruneSimilarityThreshold()).isEqualTo(0.4f);
        assertThat(dto.minClusterSize()).isEqualTo(5);
        assertThat(dto.gracePeriodHours()).isEqualTo(72);
        assertThat(dto.displayWindowDays()).isEqualTo(14);
        assertThat(dto.synthesisLimit()).isEqualTo(20);
        assertThat(dto.synthesisModel()).isEqualTo("anthropic/claude-haiku-4.5");
        assertThat(dto.deadLinkScanLimit()).isEqualTo(50);
        assertThat(dto.pruneScanLimit()).isEqualTo(60);
        assertThat(dto.sourceFailureThreshold()).isEqualTo(7);
        assertThat(dto.cohortSimilarityThreshold()).isEqualTo(0.7f);
        assertThat(dto.synthesisMinPostsPerSide()).isEqualTo(2);
        assertThat(dto.detailFetchLimit()).isEqualTo(30);
        assertThat(dto.postRetentionDays()).isEqualTo(15);
        assertThat(dto.stalePostScanLimit()).isEqualTo(300);
    }
}
