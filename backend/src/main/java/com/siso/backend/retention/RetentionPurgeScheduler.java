package com.siso.backend.retention;

import com.siso.backend.alert.AdminAlert;
import com.siso.backend.alert.AdminAlertRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * 파기 주기 자체는 인프라 설정(배포 시점 프로퍼티)이지 서비스 동작 기준이
 * 아니라 어드민 설정으로 안 둔다 — PetitionSyncScheduler와 동일한 판단.
 * 매일 한 번이면 충분한 정리 작업이라 기본값은 24시간.
 */
@Component
public class RetentionPurgeScheduler {

    private static final Logger log = LoggerFactory.getLogger(RetentionPurgeScheduler.class);

    private final RetentionPurgeService retentionPurgeService;
    private final AdminAlertRepository adminAlertRepository;

    public RetentionPurgeScheduler(
            RetentionPurgeService retentionPurgeService, AdminAlertRepository adminAlertRepository) {
        this.retentionPurgeService = retentionPurgeService;
        this.adminAlertRepository = adminAlertRepository;
    }

    @Scheduled(fixedRateString = "${app.retention.purge-interval-ms:86400000}")
    public void purge() {
        // 개인정보처리방침에 명시된 90일 파기 약속을 지키는 배치라, 실패해도
        // 아무 알림 없이 조용히 안 돌면 컴플라이언스 공백이 생겨도 아무도
        // 모른다 — 실패 시 관리자 알림을 남긴다.
        try {
            retentionPurgeService.purge();
        } catch (Exception exc) {
            log.error("개인정보 파기 배치 실패", exc);
            adminAlertRepository.save(new AdminAlert(
                    "retention_purge_failed",
                    Map.of("error", String.valueOf(exc.getMessage())),
                    OffsetDateTime.now()));
        }
    }
}
