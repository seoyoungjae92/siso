package com.siso.backend.newsletter;

import com.siso.backend.pair.TopicPair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 주간 리포트를 확인된(confirmed) 구독자 전원에게 발송한다. 구독자마다
 * 자기 자신의 수신거부 링크가 들어가야 해서 개별 발송이 필요하다(배치
 * BCC 불가). 개별 발송 실패는 건너뛰고 나머지를 계속 진행한다(크롤러/
 * PetitionSyncService와 동일한 원칙).
 */
@Service
public class NewsletterSendService {

    private static final Logger log = LoggerFactory.getLogger(NewsletterSendService.class);
    private static final String CONFIRMED = "confirmed";

    private final NewsletterSubscriberRepository newsletterSubscriberRepository;
    private final NewsletterDigestService newsletterDigestService;
    private final ResendEmailClient resendEmailClient;
    private final String frontendUrl;

    public NewsletterSendService(
            NewsletterSubscriberRepository newsletterSubscriberRepository,
            NewsletterDigestService newsletterDigestService,
            ResendEmailClient resendEmailClient,
            @Value("${app.newsletter.frontend-url}") String frontendUrl) {
        this.newsletterSubscriberRepository = newsletterSubscriberRepository;
        this.newsletterDigestService = newsletterDigestService;
        this.resendEmailClient = resendEmailClient;
        this.frontendUrl = frontendUrl;
    }

    @Transactional(readOnly = true)
    public int sendWeeklyDigest() {
        if (!resendEmailClient.isEnabled()) {
            return 0;
        }

        List<TopicPair> topPairs = newsletterDigestService.findTopPairsForDigest();
        if (topPairs.isEmpty()) {
            log.info("주간 리포트 발송 건너뜀 — 이번 주 인기 링 없음");
            return 0;
        }

        List<NewsletterSubscriber> subscribers = newsletterSubscriberRepository.findByStatus(CONFIRMED);
        int sent = 0;
        for (NewsletterSubscriber subscriber : subscribers) {
            String unsubscribeUrl = frontendUrl + "/newsletter/unsubscribe?token=" + subscriber.getToken();
            String html = newsletterDigestService.buildDigestHtml(topPairs, unsubscribeUrl);
            try {
                resendEmailClient.send(subscriber.getEmail(), "이번 주 좌우 리포트", html);
                sent++;
            } catch (NewsletterSendFailed e) {
                log.warn("주간 리포트 발송 실패(email={}): {}", subscriber.getEmail(), e.getMessage());
            }
        }
        return sent;
    }
}
