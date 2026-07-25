package com.siso.backend.petition;

import com.siso.backend.settings.PetitionSettings;
import com.siso.backend.settings.PetitionSettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * PTTRCP(청원 접수목록)로 진행중(collecting) 청원 목록을 최신 동의건수로
 * 갱신하고, 집계 기간(windowDays)이 지난 진행중 청원은 PTTINFODETAIL(청원
 * 상세정보)을 한 번 더 호출해 성립/미성립을 판정하고 closed로 전환한다.
 * 개별 항목 처리 실패는 스킵하고 나머지를 계속 진행한다(크롤러의 "합성
 * 실패는 조용히 스킵" 원칙과 동일).
 */
@Service
public class PetitionSyncService {

    private static final short SETTINGS_ID = 1;
    private static final int FETCH_PAGE_SIZE = 200;
    private static final String CITIZEN_PETITION_KIND = "국민동의";

    private final AssemblyPetitionClient assemblyPetitionClient;
    private final PetitionRepository petitionRepository;
    private final PetitionSettingsRepository petitionSettingsRepository;

    public PetitionSyncService(
            AssemblyPetitionClient assemblyPetitionClient,
            PetitionRepository petitionRepository,
            PetitionSettingsRepository petitionSettingsRepository) {
        this.assemblyPetitionClient = assemblyPetitionClient;
        this.petitionRepository = petitionRepository;
        this.petitionSettingsRepository = petitionSettingsRepository;
    }

    /**
     * synchronized: 스케줄러(자동)와 어드민 수동 트리거가 동시에 들어오면
     * 같은 신규 청원에 대해 두 트랜잭션이 동시에 INSERT를 시도해 PK 충돌이
     * 날 수 있다(실측으로 확인) — 단일 인스턴스 배포 전제라 JVM 레벨
     * synchronized로 겹쳐 실행되지 않게 막는다.
     */
    @Transactional
    public synchronized PetitionSyncResult sync() {
        PetitionSettings settings = petitionSettingsRepository.findById(SETTINGS_ID).orElseThrow();
        OffsetDateTime now = OffsetDateTime.now();

        int upserted = syncList(settings, now);
        int closed = closeExpired(settings, now);

        return new PetitionSyncResult(upserted, closed, now);
    }

    private int syncList(PetitionSettings settings, OffsetDateTime now) {
        List<AssemblyPetitionRow> rows;
        try {
            rows = assemblyPetitionClient.fetchList(settings.getEraco(), FETCH_PAGE_SIZE);
        } catch (Exception e) {
            return 0;
        }

        int count = 0;
        for (AssemblyPetitionRow row : rows) {
            if (upsert(row, settings.getEraco(), now)) {
                count++;
            }
        }
        return count;
    }

    private boolean upsert(AssemblyPetitionRow row, String eraco, OffsetDateTime now) {
        if (!CITIZEN_PETITION_KIND.equals(row.kind())) {
            return false;
        }
        if (row.pttId() == null || row.pttNo() == null || row.title() == null || row.linkUrl() == null) {
            return false;
        }
        long agreeCount = parseCount(row.agreeCountRaw());
        if (agreeCount < 0) {
            return false;
        }
        LocalDate receivedAt = parseDate(row.receivedAt());
        if (receivedAt == null) {
            return false;
        }

        Petition existing = petitionRepository.findById(row.pttId()).orElse(null);
        if (existing == null) {
            petitionRepository.save(new Petition(
                    row.pttId(), eraco, row.pttNo(), row.title(), agreeCount, receivedAt, row.linkUrl(), now));
            return true;
        }
        if (Petition.STATUS_COLLECTING.equals(existing.getStatus())) {
            existing.refresh(agreeCount, now);
            return true;
        }
        return false;
    }

    private int closeExpired(PetitionSettings settings, OffsetDateTime now) {
        LocalDate cutoff = now.toLocalDate().minusDays(settings.getWindowDays());
        List<Petition> expired =
                petitionRepository.findByStatusAndReceivedAtBefore(Petition.STATUS_COLLECTING, cutoff);

        int count = 0;
        for (Petition petition : expired) {
            if (closeOne(petition, now)) {
                count++;
            }
        }
        return count;
    }

    private boolean closeOne(Petition petition, OffsetDateTime now) {
        AssemblyPetitionDetail detail;
        try {
            detail = assemblyPetitionClient.fetchDetail(petition.getPttId());
        } catch (Exception e) {
            return false;
        }
        if (detail == null) {
            return false;
        }

        LocalDate committeeReferredAt = parseDate(detail.committeeReferredAt());
        String outcome =
                committeeReferredAt != null ? Petition.OUTCOME_ESTABLISHED : Petition.OUTCOME_NOT_ESTABLISHED;
        Float achvRatio = parseRatio(detail.achvRatioRaw());

        petition.close(outcome, detail.committeeName(), committeeReferredAt, achvRatio, now);
        return true;
    }

    private long parseCount(String raw) {
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

    private Float parseRatio(String raw) {
        if (raw == null) {
            return null;
        }
        try {
            return Float.parseFloat(raw.replace(",", "").trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
