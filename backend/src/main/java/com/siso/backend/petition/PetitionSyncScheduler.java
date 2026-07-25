package com.siso.backend.petition;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 청원 동기화 주기. 이 값은 서비스 동작 기준(어드민 조작 대상)이라기보다
 * 외부 API 호출 빈도를 정하는 인프라 설정이라(크롤러 cron 주기와 같은
 * 성격) 어드민 설정이 아니라 배포 시점 프로퍼티로 둔다.
 */
@Component
public class PetitionSyncScheduler {

    private final PetitionSyncService petitionSyncService;

    public PetitionSyncScheduler(PetitionSyncService petitionSyncService) {
        this.petitionSyncService = petitionSyncService;
    }

    @Scheduled(fixedRateString = "${app.petition.sync-interval-ms:1800000}")
    public void sync() {
        petitionSyncService.sync();
    }
}
