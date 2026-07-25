package com.siso.backend.petition;

import com.siso.backend.settings.PetitionSettings;
import com.siso.backend.settings.PetitionSettingsRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PetitionServiceTest {

    @Mock
    private AssemblyPetitionClient assemblyPetitionClient;

    @Mock
    private PetitionSettingsRepository petitionSettingsRepository;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    private PetitionService newService() {
        return new PetitionService(assemblyPetitionClient, petitionSettingsRepository, redisTemplate, jsonMapper);
    }

    private PetitionSettings settings(String eraco, int topN, int windowDays, int cacheTtlMinutes) {
        PetitionSettings settings = mock(PetitionSettings.class);
        lenient().when(settings.getEraco()).thenReturn(eraco);
        lenient().when(settings.getTopN()).thenReturn(topN);
        lenient().when(settings.getWindowDays()).thenReturn(windowDays);
        lenient().when(settings.getCacheTtlMinutes()).thenReturn(cacheTtlMinutes);
        return settings;
    }

    @Test
    void get_cacheHit_returnsCachedWithoutCallingClient() {
        PetitionSettings settings = settings("제22대", 10, 30, 30);
        when(petitionSettingsRepository.findById((short) 1)).thenReturn(Optional.of(settings));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("petitions:top:제22대"))
                .thenReturn(
                        "[{\"id\":\"P1\",\"title\":\"t\",\"agreeCount\":100,\"receivedAt\":\"2026-07-20\",\"linkUrl\":\"https://x\"}]");

        List<PetitionDto> result = newService().getTop();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo("P1");
        verify(assemblyPetitionClient, never()).fetchList(anyString(), anyInt());
    }

    @Test
    void get_cacheMiss_filtersAndSortsAndCaches() {
        PetitionSettings settings = settings("제22대", 2, 30, 30);
        when(petitionSettingsRepository.findById((short) 1)).thenReturn(Optional.of(settings));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);

        LocalDate recent = LocalDate.now().minusDays(1);
        LocalDate tooOld = LocalDate.now().minusDays(40);

        when(assemblyPetitionClient.fetchList("제22대", 200))
                .thenReturn(List.of(
                        new AssemblyPetitionRow("P1", "낮은 동의", "국민동의", "1,000", recent.toString(), "https://a"),
                        new AssemblyPetitionRow("P2", "높은 동의", "국민동의", "50,000", recent.toString(), "https://b"),
                        new AssemblyPetitionRow("P3", "중간 동의", "국민동의", "10,000", recent.toString(), "https://c"),
                        new AssemblyPetitionRow("P4", "의원소개라 제외", "의원소개", null, recent.toString(), "https://d"),
                        new AssemblyPetitionRow("P5", "기간 밖이라 제외", "국민동의", "99,000", tooOld.toString(), "https://e")));

        List<PetitionDto> result = newService().getTop();

        assertThat(result).extracting(PetitionDto::id).containsExactly("P2", "P3");
        verify(valueOperations).set(eq("petitions:top:제22대"), anyString(), eq(Duration.ofMinutes(30)));
    }

    @Test
    void get_clientThrows_returnsEmptyAndDoesNotCache() {
        PetitionSettings settings = settings("제22대", 10, 30, 30);
        when(petitionSettingsRepository.findById((short) 1)).thenReturn(Optional.of(settings));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(assemblyPetitionClient.fetchList(anyString(), anyInt()))
                .thenThrow(new RuntimeException("network error"));

        List<PetitionDto> result = newService().getTop();

        assertThat(result).isEmpty();
        verify(valueOperations, never()).set(anyString(), anyString(), any(Duration.class));
    }
}
