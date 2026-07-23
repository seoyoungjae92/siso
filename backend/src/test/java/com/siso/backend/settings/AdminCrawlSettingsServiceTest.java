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
    }

    @Test
    void update_overwritesAllFields() {
        CrawlSettings settings = defaults();
        when(crawlSettingsRepository.findById((short) 1)).thenReturn(Optional.of(settings));

        CrawlSettingsRequest request = new CrawlSettingsRequest(0.6f, 0.4f, 5, 72, 14);
        CrawlSettingsDto dto = newService().update(request);

        assertThat(dto.matchSimilarityThreshold()).isEqualTo(0.6f);
        assertThat(dto.pruneSimilarityThreshold()).isEqualTo(0.4f);
        assertThat(dto.minClusterSize()).isEqualTo(5);
        assertThat(dto.gracePeriodHours()).isEqualTo(72);
        assertThat(dto.displayWindowDays()).isEqualTo(14);
    }
}
