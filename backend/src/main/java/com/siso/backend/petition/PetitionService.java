package com.siso.backend.petition;

import com.siso.backend.settings.PetitionSettings;
import com.siso.backend.settings.PetitionSettingsRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 청원 데이터는 더 이상 요청 시점에 실시간으로 국회 API를 호출하지 않고,
 * PetitionSyncService(배치)가 미리 채워둔 petitions 테이블을 그대로 읽기만
 * 한다 — posts/topic_pairs가 크롤러 배치로 채워진 걸 API가 읽기만 하는
 * 것과 동일한 구조.
 */
@Service
public class PetitionService {

    private static final short SETTINGS_ID = 1;

    private final PetitionRepository petitionRepository;
    private final PetitionSettingsRepository petitionSettingsRepository;

    public PetitionService(
            PetitionRepository petitionRepository, PetitionSettingsRepository petitionSettingsRepository) {
        this.petitionRepository = petitionRepository;
        this.petitionSettingsRepository = petitionSettingsRepository;
    }

    @Transactional(readOnly = true)
    public List<PetitionDto> getTop() {
        PetitionSettings settings = petitionSettingsRepository.findById(SETTINGS_ID).orElseThrow();
        Pageable pageable = PageRequest.of(0, settings.getTopN());
        return petitionRepository.findByStatusOrderByAgreeCountDesc(Petition.STATUS_COLLECTING, pageable).stream()
                .map(this::toDto)
                .toList();
    }

    private PetitionDto toDto(Petition petition) {
        return new PetitionDto(
                petition.getPttId(),
                petition.getTitle(),
                petition.getAgreeCount(),
                petition.getReceivedAt().toString(),
                petition.getLinkUrl());
    }
}
