package com.siso.backend.moderation;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * LLM 신고 분류 배치 주기. PetitionSyncScheduler/RetentionPurgeScheduler와
 * 동일하게 서비스 동작 기준이 아니라 외부 API 호출 빈도(비용/레이트리밋)를
 * 정하는 인프라 설정이라 어드민 설정이 아니라 배포 시점 프로퍼티로 둔다.
 */
@Component
public class ReportClassificationScheduler {

    private final ReportClassificationService reportClassificationService;

    public ReportClassificationScheduler(ReportClassificationService reportClassificationService) {
        this.reportClassificationService = reportClassificationService;
    }

    @Scheduled(fixedRateString = "${app.moderation.llm-classify-interval-ms:1800000}")
    public void classify() {
        reportClassificationService.classifyPending();
    }
}
