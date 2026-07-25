package com.siso.backend.settings;

import com.siso.backend.admin.AdminPetitionSettingsService;
import com.siso.backend.admin.PetitionSettingsDto;
import com.siso.backend.admin.PetitionSettingsRequest;
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
class AdminPetitionSettingsServiceTest {

    @Mock
    private PetitionSettingsRepository petitionSettingsRepository;

    private AdminPetitionSettingsService newService() {
        return new AdminPetitionSettingsService(petitionSettingsRepository);
    }

    private PetitionSettings defaults() {
        PetitionSettings settings = new PetitionSettings();
        ReflectionTestUtils.setField(settings, "id", (short) 1);
        ReflectionTestUtils.setField(settings, "eraco", "제22대");
        ReflectionTestUtils.setField(settings, "topN", 10);
        ReflectionTestUtils.setField(settings, "windowDays", 30);
        ReflectionTestUtils.setField(settings, "updatedAt", OffsetDateTime.now());
        return settings;
    }

    @Test
    void get_returnsCurrentValues() {
        when(petitionSettingsRepository.findById((short) 1)).thenReturn(Optional.of(defaults()));

        PetitionSettingsDto dto = newService().get();

        assertThat(dto.eraco()).isEqualTo("제22대");
        assertThat(dto.topN()).isEqualTo(10);
    }

    @Test
    void update_overwritesAllFields() {
        PetitionSettings settings = defaults();
        when(petitionSettingsRepository.findById((short) 1)).thenReturn(Optional.of(settings));

        PetitionSettingsRequest request = new PetitionSettingsRequest("제23대", 15, 45);
        PetitionSettingsDto dto = newService().update(request);

        assertThat(dto.eraco()).isEqualTo("제23대");
        assertThat(dto.topN()).isEqualTo(15);
        assertThat(dto.windowDays()).isEqualTo(45);
    }
}
