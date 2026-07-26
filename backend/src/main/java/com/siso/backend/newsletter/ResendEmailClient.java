package com.siso.backend.newsletter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Resend(https://resend.com) API로 트랜잭션 이메일(구독 확인, 주간
 * 리포트)을 발송한다. 다른 외부 API 클라이언트(AssemblyPetitionClient,
 * OpenRouterReportClassifier)와 동일하게 SDK 없이 raw RestClient로 직접
 * 호출 — 이 코드베이스 전체의 관행.
 *
 * 발신 도메인이 Resend에 인증되지 않으면 실제 대량 발송은 안 되고
 * onboarding@resend.dev 같은 테스트 발신자로만 제한적으로 동작한다 —
 * 서비스 도메인 확정(CLAUDE.md D7) 후 도메인 인증이 필요.
 */
@Component
public class ResendEmailClient {

    private static final String BASE_URL = "https://api.resend.com";
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private final RestClient restClient;
    private final String apiKey;
    private final String fromAddress;

    public ResendEmailClient(
            RestClient.Builder builder,
            @Value("${app.newsletter.resend-api-key}") String apiKey,
            @Value("${app.newsletter.from-address}") String fromAddress) {
        HttpClientSettings settings =
                HttpClientSettings.defaults().withConnectTimeout(TIMEOUT).withReadTimeout(TIMEOUT);
        ClientHttpRequestFactory requestFactory = ClientHttpRequestFactoryBuilder.detect().build(settings);
        this.restClient = builder.baseUrl(BASE_URL).requestFactory(requestFactory).build();
        this.apiKey = apiKey;
        this.fromAddress = fromAddress;
    }

    public boolean isEnabled() {
        return apiKey != null && !apiKey.isBlank();
    }

    public void send(String to, String subject, String html) {
        if (!isEnabled()) {
            throw new NewsletterSendFailed("RESEND_API_KEY 미설정");
        }

        Map<String, Object> body =
                Map.of("from", fromAddress, "to", List.of(to), "subject", subject, "html", html);

        try {
            restClient
                    .post()
                    .uri("/emails")
                    .header("Authorization", "Bearer " + apiKey)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            throw new NewsletterSendFailed("Resend 발송 실패: " + e.getMessage(), e);
        }
    }
}
