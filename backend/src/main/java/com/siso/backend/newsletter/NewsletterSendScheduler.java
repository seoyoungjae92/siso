package com.siso.backend.newsletter;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 주간 리포트 발송 주기. PetitionSyncScheduler와 동일하게 배포시점
 * 프로퍼티(cron)로 둔다 — "매주 월요일 9시" 같은 발송 시점은 서비스
 * 정책이라기보다 운영 스케줄이라 어드민 설정 대상이 아님.
 */
@Component
public class NewsletterSendScheduler {

    private final NewsletterSendService newsletterSendService;

    public NewsletterSendScheduler(NewsletterSendService newsletterSendService) {
        this.newsletterSendService = newsletterSendService;
    }

    @Scheduled(cron = "${app.newsletter.send-cron:0 0 9 * * MON}")
    public void sendWeeklyDigest() {
        newsletterSendService.sendWeeklyDigest();
    }
}
