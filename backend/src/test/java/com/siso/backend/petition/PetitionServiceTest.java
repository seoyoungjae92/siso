package com.siso.backend.petition;

import com.siso.backend.settings.PetitionSettings;
import com.siso.backend.settings.PetitionSettingsRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PetitionServiceTest {

    @Mock
    private PetitionRepository petitionRepository;

    @Mock
    private PetitionSettingsRepository petitionSettingsRepository;

    private PetitionService newService() {
        return new PetitionService(petitionRepository, petitionSettingsRepository);
    }

    private PetitionSettings settings(int topN) {
        PetitionSettings settings = mock(PetitionSettings.class);
        when(settings.getTopN()).thenReturn(topN);
        return settings;
    }

    @Test
    void getTop_readsCollectingPetitionsOrderedByAgreeCountFromRepository() {
        PetitionSettings settings = settings(10);
        when(petitionSettingsRepository.findById((short) 1)).thenReturn(Optional.of(settings));

        Petition petition = new Petition(
                "P1", "제22대", "2200321", "제목", 53728L, LocalDate.of(2026, 7, 23), "https://a", OffsetDateTime.now());
        when(petitionRepository.findByStatusOrderByAgreeCountDesc(eq(Petition.STATUS_COLLECTING), any()))
                .thenReturn(List.of(petition));

        List<PetitionDto> result = newService().getTop();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo("P1");
        assertThat(result.get(0).agreeCount()).isEqualTo(53728L);
        assertThat(result.get(0).receivedAt()).isEqualTo("2026-07-23");
    }

    @Test
    void getTop_noCollectingPetitions_returnsEmpty() {
        PetitionSettings settings = settings(10);
        when(petitionSettingsRepository.findById((short) 1)).thenReturn(Optional.of(settings));
        when(petitionRepository.findByStatusOrderByAgreeCountDesc(eq(Petition.STATUS_COLLECTING), any()))
                .thenReturn(List.of());

        List<PetitionDto> result = newService().getTop();

        assertThat(result).isEmpty();
    }
}
