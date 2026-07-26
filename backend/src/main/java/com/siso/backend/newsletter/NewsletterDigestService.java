package com.siso.backend.newsletter;

import com.siso.backend.pair.TopicPair;
import com.siso.backend.pair.TopicPairRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;

/** '주간 좌우 리포트' 본문(HTML) 생성 — 최근 7일 인기 링 TOP 5. */
@Service
public class NewsletterDigestService {

    private static final int TOP_N = 5;
    private static final int LOOKBACK_DAYS = 7;

    private final TopicPairRepository topicPairRepository;
    private final String frontendUrl;

    public NewsletterDigestService(
            TopicPairRepository topicPairRepository, @Value("${app.newsletter.frontend-url}") String frontendUrl) {
        this.topicPairRepository = topicPairRepository;
        this.frontendUrl = frontendUrl;
    }

    /** 발송할 정도로 콘텐츠가 있으면 top pair 목록을, 없으면 빈 리스트를 반환한다. */
    public List<TopicPair> findTopPairsForDigest() {
        OffsetDateTime since = OffsetDateTime.now().minusDays(LOOKBACK_DAYS);
        return topicPairRepository.findTopEngagementSince(since, PageRequest.of(0, TOP_N));
    }

    public String buildDigestHtml(List<TopicPair> topPairs, String unsubscribeUrl) {
        StringBuilder items = new StringBuilder();
        for (TopicPair pair : topPairs) {
            items.append(
                    """
                    <li style="margin-bottom:16px;">
                      <a href="%s/pairs/%d" style="color:#1B1B22;font-weight:bold;text-decoration:none;">%s</a>
                    </li>
                    """
                            .formatted(frontendUrl, pair.getId(), escape(pair.getTitle())));
        }

        return """
                <div style="font-family: sans-serif; max-width: 480px; margin: 0 auto;">
                  <h2>이번 주 좌우 리포트</h2>
                  <p style="color:#6B6960;">이번 주 시소에서 가장 활발했던 토론 주제입니다.</p>
                  <ol style="padding-left:20px;">
                    %s
                  </ol>
                  <p><a href="%s" style="color:#8A877E;font-size:12px;">전체 토론 보러가기</a></p>
                  <hr style="border:none;border-top:1px solid #E3E1DB;margin:24px 0;" />
                  <p style="color:#8A877E;font-size:11px;">
                    이 메일은 시소 뉴스레터 구독자에게 발송되었습니다.
                    <a href="%s" style="color:#8A877E;">수신거부</a>
                  </p>
                </div>
                """
                .formatted(items, frontendUrl, unsubscribeUrl);
    }

    private String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
