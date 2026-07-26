package com.siso.backend.settings;

import com.siso.backend.admin.AdminElectionSettingsService;
import com.siso.backend.admin.ElectionSettingsDto;
import com.siso.backend.admin.ElectionSettingsRequest;
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
class AdminElectionSettingsServiceTest {

    @Mock
    private ElectionSettingsRepository electionSettingsRepository;

    private AdminElectionSettingsService newService() {
        return new AdminElectionSettingsService(electionSettingsRepository);
    }

    private ElectionSettings defaults() {
        ElectionSettings settings = new ElectionSettings();
        ReflectionTestUtils.setField(settings, "id", (short) 1);
        ReflectionTestUtils.setField(settings, "enabled", false);
        ReflectionTestUtils.setField(settings, "overrideAutoBlindThreshold", 5);
        ReflectionTestUtils.setField(settings, "updatedAt", OffsetDateTime.now());
        return settings;
    }

    @Test
    void get_returnsCurrentValue() {
        when(electionSettingsRepository.findById((short) 1)).thenReturn(Optional.of(defaults()));

        ElectionSettingsDto dto = newService().get();

        assertThat(dto.enabled()).isFalse();
        assertThat(dto.overrideAutoBlindThreshold()).isEqualTo(5);
    }

    @Test
    void update_enablesElectionModeAndOverridesThreshold() {
        ElectionSettings settings = defaults();
        when(electionSettingsRepository.findById((short) 1)).thenReturn(Optional.of(settings));

        ElectionSettingsDto dto = newService().update(new ElectionSettingsRequest(true, 3));

        assertThat(dto.enabled()).isTrue();
        assertThat(dto.overrideAutoBlindThreshold()).isEqualTo(3);
    }
}
