package com.siso.backend.retention;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 파기 주기 자체는 인프라 설정(배포 시점 프로퍼티)이지 서비스 동작 기준이
 * 아니라 어드민 설정으로 안 둔다 — PetitionSyncScheduler와 동일한 판단.
 * 매일 한 번이면 충분한 정리 작업이라 기본값은 24시간.
 */
@Component
public class RetentionPurgeScheduler {

    private final RetentionPurgeService retentionPurgeService;

    public RetentionPurgeScheduler(RetentionPurgeService retentionPurgeService) {
        this.retentionPurgeService = retentionPurgeService;
    }

    @Scheduled(fixedRateString = "${app.retention.purge-interval-ms:86400000}")
    public void purge() {
        retentionPurgeService.purge();
    }
}
