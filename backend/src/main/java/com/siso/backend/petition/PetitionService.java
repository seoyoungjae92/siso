package com.siso.backend.petition;

import com.siso.backend.settings.PetitionSettings;
import com.siso.backend.settings.PetitionSettingsRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * PTTRCP(청원 접수목록) 응답에서 "실시간 인기 청원" 랭킹에 쓸 항목만
 * 골라낸다. 이 API엔 "지금 접수중"인지 알려주는 상태 필드가 없어서,
 * 국민동의청원 제도상 접수일로부터 30일간만 동의를 받는다는 점을 이용해
 * windowDays 이내 접수 건만 "진행중"으로 근사한다. 의원소개 청원은
 * 동의 집계 자체가 없어(CITZN_AGM_CNT=null) 랭킹 대상에서 제외.
 */
@Service
public class PetitionService {

    private static final String CACHE_KEY_PREFIX = "petitions:top:";
    private static final short SETTINGS_ID = 1;
    private static final int FETCH_PAGE_SIZE = 200;
    private static final String CITIZEN_PETITION_KIND = "국민동의";

    private final AssemblyPetitionClient assemblyPetitionClient;
    private final PetitionSettingsRepository petitionSettingsRepository;
    private final StringRedisTemplate redisTemplate;
    private final JsonMapper jsonMapper;

    public PetitionService(
            AssemblyPetitionClient assemblyPetitionClient,
            PetitionSettingsRepository petitionSettingsRepository,
            StringRedisTemplate redisTemplate,
            JsonMapper jsonMapper) {
        this.assemblyPetitionClient = assemblyPetitionClient;
        this.petitionSettingsRepository = petitionSettingsRepository;
        this.redisTemplate = redisTemplate;
        this.jsonMapper = jsonMapper;
    }

    public List<PetitionDto> getTop() {
        PetitionSettings settings = petitionSettingsRepository.findById(SETTINGS_ID).orElseThrow();
        String cacheKey = CACHE_KEY_PREFIX + settings.getEraco();

        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return deserialize(cached);
        }

        List<PetitionDto> fresh = fetchAndRank(settings);
        if (!fresh.isEmpty()) {
            String serialized = serialize(fresh);
            if (serialized != null) {
                redisTemplate.opsForValue().set(cacheKey, serialized, Duration.ofMinutes(settings.getCacheTtlMinutes()));
            }
        }
        return fresh;
    }

    private List<PetitionDto> fetchAndRank(PetitionSettings settings) {
        List<AssemblyPetitionRow> rows;
        try {
            rows = assemblyPetitionClient.fetchList(settings.getEraco(), FETCH_PAGE_SIZE);
        } catch (Exception e) {
            return List.of();
        }

        LocalDate cutoff = LocalDate.now().minusDays(settings.getWindowDays());

        List<PetitionDto> ranked = new ArrayList<>();
        for (AssemblyPetitionRow row : rows) {
            PetitionDto dto = toDtoIfEligible(row, cutoff);
            if (dto != null) {
                ranked.add(dto);
            }
        }

        ranked.sort(Comparator.comparingLong(PetitionDto::agreeCount).reversed());
        return ranked.size() > settings.getTopN() ? ranked.subList(0, settings.getTopN()) : ranked;
    }

    private PetitionDto toDtoIfEligible(AssemblyPetitionRow row, LocalDate cutoff) {
        if (!CITIZEN_PETITION_KIND.equals(row.kind())) {
            return null;
        }
        if (row.pttId() == null || row.title() == null || row.linkUrl() == null) {
            return null;
        }

        long agreeCount = parseAgreeCount(row.agreeCountRaw());
        if (agreeCount < 0) {
            return null;
        }

        LocalDate receivedAt = parseDate(row.receivedAt());
        if (receivedAt == null || receivedAt.isBefore(cutoff)) {
            return null;
        }

        return new PetitionDto(row.pttId(), row.title(), agreeCount, receivedAt.toString(), row.linkUrl());
    }

    private long parseAgreeCount(String raw) {
        if (raw == null) {
            return -1;
        }
        try {
            return Long.parseLong(raw.replace(",", "").trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private LocalDate parseDate(String raw) {
        if (raw == null) {
            return null;
        }
        try {
            return LocalDate.parse(raw);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private String serialize(List<PetitionDto> petitions) {
        try {
            return jsonMapper.writeValueAsString(petitions);
        } catch (Exception e) {
            return null;
        }
    }

    private List<PetitionDto> deserialize(String json) {
        try {
            return jsonMapper.readValue(json, new TypeReference<List<PetitionDto>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }
}
