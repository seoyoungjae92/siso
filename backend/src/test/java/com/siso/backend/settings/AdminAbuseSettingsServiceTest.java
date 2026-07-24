package com.siso.backend.settings;

import com.siso.backend.admin.AbuseSettingsDto;
import com.siso.backend.admin.AbuseSettingsRequest;
import com.siso.backend.admin.AdminAbuseSettingsService;
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
class AdminAbuseSettingsServiceTest {

    @Mock
    private AbuseSettingsRepository abuseSettingsRepository;

    private AdminAbuseSettingsService newService() {
        return new AdminAbuseSettingsService(abuseSettingsRepository);
    }

    private AbuseSettings defaults() {
        AbuseSettings settings = new AbuseSettings();
        ReflectionTestUtils.setField(settings, "id", (short) 1);
        ReflectionTestUtils.setField(settings, "multiAccountClusterSize", 4);
        ReflectionTestUtils.setField(settings, "multiAccountTrustPenaltyMultiplier", 0.3f);
        ReflectionTestUtils.setField(settings, "trustMaturityHours", 72);
        ReflectionTestUtils.setField(settings, "trustMinWeight", 0.3f);
        ReflectionTestUtils.setField(settings, "duplicateSimilarityThreshold", 0.85f);
        ReflectionTestUtils.setField(settings, "duplicateLookbackCount", 10);
        ReflectionTestUtils.setField(settings, "duplicateLookbackMinutes", 1440);
        ReflectionTestUtils.setField(settings, "spikeWindowMinutes", 10);
        ReflectionTestUtils.setField(settings, "spikeVoteThreshold", 30);
        ReflectionTestUtils.setField(settings, "spikeReactionThreshold", 30);
        ReflectionTestUtils.setField(settings, "updatedAt", OffsetDateTime.now());
        return settings;
    }

    @Test
    void get_returnsCurrentValues() {
        when(abuseSettingsRepository.findById((short) 1)).thenReturn(Optional.of(defaults()));

        AbuseSettingsDto dto = newService().get();

        assertThat(dto.multiAccountClusterSize()).isEqualTo(4);
        assertThat(dto.spikeVoteThreshold()).isEqualTo(30);
    }

    @Test
    void update_overwritesAllFields() {
        AbuseSettings settings = defaults();
        when(abuseSettingsRepository.findById((short) 1)).thenReturn(Optional.of(settings));

        AbuseSettingsRequest request =
                new AbuseSettingsRequest(5, 0.2f, 48, 0.2f, 0.9f, 5, 720, 15, 50, 50);
        AbuseSettingsDto dto = newService().update(request);

        assertThat(dto.multiAccountClusterSize()).isEqualTo(5);
        assertThat(dto.spikeVoteThreshold()).isEqualTo(50);
        assertThat(dto.duplicateSimilarityThreshold()).isEqualTo(0.9f);
    }
}
