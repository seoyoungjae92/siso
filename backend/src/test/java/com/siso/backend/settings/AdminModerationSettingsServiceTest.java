package com.siso.backend.settings;

import com.siso.backend.admin.AdminModerationSettingsService;
import com.siso.backend.admin.ModerationSettingsDto;
import com.siso.backend.admin.ModerationSettingsRequest;
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
class AdminModerationSettingsServiceTest {

    @Mock
    private ModerationSettingsRepository moderationSettingsRepository;

    private AdminModerationSettingsService newService() {
        return new AdminModerationSettingsService(moderationSettingsRepository);
    }

    private ModerationSettings defaults() {
        ModerationSettings settings = new ModerationSettings();
        ReflectionTestUtils.setField(settings, "id", (short) 1);
        ReflectionTestUtils.setField(settings, "autoBlindReportThreshold", 20);
        ReflectionTestUtils.setField(settings, "updatedAt", OffsetDateTime.now());
        return settings;
    }

    @Test
    void get_returnsCurrentValue() {
        when(moderationSettingsRepository.findById((short) 1)).thenReturn(Optional.of(defaults()));

        ModerationSettingsDto dto = newService().get();

        assertThat(dto.autoBlindReportThreshold()).isEqualTo(20);
    }

    @Test
    void update_overwritesThreshold() {
        ModerationSettings settings = defaults();
        when(moderationSettingsRepository.findById((short) 1)).thenReturn(Optional.of(settings));

        ModerationSettingsDto dto = newService().update(new ModerationSettingsRequest(10));

        assertThat(dto.autoBlindReportThreshold()).isEqualTo(10);
    }
}
