package com.siso.backend.newsletter;

import com.siso.backend.ratelimit.RateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * 뉴스레터 구독/확인/수신거부. 더블 옵트인(D12 확장 논의) — 신청만으로는
 * pending 상태이고, 확인 메일의 링크를 눌러야 confirmed로 바뀐다. 정보통신망법상
 * 수신동의 입증 + 스팸신고로 인한 발신 평판 저하 방지 둘 다를 위함.
 */
@Service
public class NewsletterService {

    private static final Logger log = LoggerFactory.getLogger(NewsletterService.class);
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final String PENDING = "pending";
    private static final String CONFIRMED = "confirmed";

    private final NewsletterSubscriberRepository newsletterSubscriberRepository;
    private final ResendEmailClient resendEmailClient;
    private final RateLimiter rateLimiter;
    private final String frontendUrl;

    public NewsletterService(
            NewsletterSubscriberRepository newsletterSubscriberRepository,
            ResendEmailClient resendEmailClient,
            RateLimiter rateLimiter,
            @Value("${app.newsletter.frontend-url}") String frontendUrl) {
        this.newsletterSubscriberRepository = newsletterSubscriberRepository;
        this.resendEmailClient = resendEmailClient;
        this.rateLimiter = rateLimiter;
        this.frontendUrl = frontendUrl;
    }

    @Transactional
    public void subscribe(String email) {
        if (email == null || !EMAIL_PATTERN.matcher(email).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "올바른 이메일 주소를 입력해주세요");
        }

        // anon_id가 없는 요청이라 이메일 자체를 키로 쓴다 — 특정 피해자 이메일에
        // 확인 메일을 반복 발송(메일폭탄)하는 걸 막는다.
        rateLimiter.checkOrThrow("newsletter-subscribe", email);

        Optional<NewsletterSubscriber> existing = newsletterSubscriberRepository.findByEmail(email);
        UUID token;
        if (existing.isPresent()) {
            NewsletterSubscriber subscriber = existing.get();
            if (CONFIRMED.equals(subscriber.getStatus())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미 구독 중인 이메일입니다");
            }
            token = UUID.randomUUID();
            subscriber.resetForResubscribe(token);
        } else {
            token = UUID.randomUUID();
            newsletterSubscriberRepository.save(new NewsletterSubscriber(email, token, OffsetDateTime.now()));
        }

        try {
            resendEmailClient.send(email, "시소 뉴스레터 구독을 확인해주세요", confirmationEmailHtml(token));
        } catch (NewsletterSendFailed e) {
            // 구독 자체는 저장됨 — 발송 인프라(Resend 키/도메인) 미설정이어도
            // 신청 기록은 남아서 나중에 재발송 가능. 사용자에게는 그대로
            // 성공으로 보여준다(내부 인프라 문제를 노출하지 않음).
            log.warn("구독 확인 메일 발송 실패(email={}): {}", email, e.getMessage());
        }
    }

    @Transactional
    public void confirm(String rawToken) {
        NewsletterSubscriber subscriber = findByToken(rawToken);
        if (!PENDING.equals(subscriber.getStatus())) {
            return;
        }
        subscriber.confirm(OffsetDateTime.now());
    }

    // 개인정보처리방침: 수신거부 시 이메일 주소를 더 이상 보관하지 않는다 —
    // 상태만 바꾸는 게 아니라 레코드 자체를 삭제한다.
    @Transactional
    public void unsubscribe(String rawToken) {
        NewsletterSubscriber subscriber = findByToken(rawToken);
        newsletterSubscriberRepository.delete(subscriber);
    }

    private NewsletterSubscriber findByToken(String rawToken) {
        UUID token;
        try {
            token = UUID.fromString(rawToken);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "유효하지 않은 링크입니다");
        }
        return newsletterSubscriberRepository
                .findByToken(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "유효하지 않은 링크입니다"));
    }

    private String confirmationEmailHtml(UUID token) {
        String confirmUrl = frontendUrl + "/newsletter/confirm?token=" + token;
        return """
                <div style="font-family: sans-serif; max-width: 480px; margin: 0 auto;">
                  <h2>시소 뉴스레터 구독을 확인해주세요</h2>
                  <p>아래 버튼을 누르면 '주간 좌우 리포트' 구독이 완료됩니다.</p>
                  <p><a href="%s" style="display:inline-block;padding:12px 20px;background:#6E3D74;color:#fff;text-decoration:none;border-radius:8px;">구독 확인하기</a></p>
                  <p style="color:#8A877E;font-size:12px;">본인이 신청하지 않았다면 이 메일을 무시하셔도 됩니다.</p>
                </div>
                """
                .formatted(confirmUrl);
    }
}
