package com.siso.backend.moderation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.Map;

/**
 * 댓글 작성 시 reCAPTCHA 점수 기반 검증(§6). 구글이 2024년부터 신규 사이트에
 * 예전 방식(secret key + siteverify)이 아니라 reCAPTCHA Enterprise
 * Assessment API로만 키를 발급해서(레거시 시크릿 키는 별도 조회가 필요하고
 * 단계적으로 막히는 중, 2026-08-08 실측) 이 API로 구현한다 — 참고로 무료
 * 등급(Essentials)도 API 자체는 Enterprise와 동일하게 씀, 등급은 콘솔에서
 * 키 만들 때 선택하는 요금제일 뿐 API가 갈리는 게 아님.
 *
 * RECAPTCHA_API_KEY/PROJECT_ID/SITE_KEY 중 하나라도 비어있으면(계정/키
 * 미설정) 검증 자체를 건너뛴다 — 다른 선택적 외부 연동과 동일한 패턴.
 * Google 쪽 장애·네트워크 오류 시엔 fail-open(허용)한다 — CAPTCHA는 보조
 * 방어선일 뿐이라 이것 때문에 댓글 작성 자체가 막히면 안 된다. 반대로
 * 검증에 정상 도달했는데 점수가 낮거나 토큰이 무효면 fail-closed(차단).
 */
@Component
public class RecaptchaVerifier {

    private static final Logger log = LoggerFactory.getLogger(RecaptchaVerifier.class);
    private static final String BASE_URL = "https://recaptchaenterprise.googleapis.com";
    private static final Duration TIMEOUT = Duration.ofSeconds(5);
    private static final String EXPECTED_ACTION = "comment";

    private final RestClient restClient;
    private final JsonMapper jsonMapper;
    private final String apiKey;
    private final String projectId;
    private final String siteKey;
    private final double minScore;

    public RecaptchaVerifier(
            RestClient.Builder builder,
            JsonMapper jsonMapper,
            @Value("${app.recaptcha.api-key:}") String apiKey,
            @Value("${app.recaptcha.project-id:}") String projectId,
            @Value("${app.recaptcha.site-key:}") String siteKey,
            @Value("${app.recaptcha.min-score:0.5}") double minScore) {
        HttpClientSettings settings =
                HttpClientSettings.defaults().withConnectTimeout(TIMEOUT).withReadTimeout(TIMEOUT);
        ClientHttpRequestFactory requestFactory = ClientHttpRequestFactoryBuilder.detect().build(settings);
        this.restClient = builder.baseUrl(BASE_URL).requestFactory(requestFactory).build();
        this.jsonMapper = jsonMapper;
        this.apiKey = apiKey;
        this.projectId = projectId;
        this.siteKey = siteKey;
        this.minScore = minScore;
    }

    public boolean isEnabled() {
        return !apiKey.isBlank() && !projectId.isBlank() && !siteKey.isBlank();
    }

    public boolean verify(String token) {
        if (!isEnabled()) {
            return true;
        }
        if (token == null || token.isBlank()) {
            return false;
        }

        Map<String, Object> requestBody =
                Map.of("event", Map.of("token", token, "siteKey", siteKey, "expectedAction", EXPECTED_ACTION));

        String raw;
        try {
            raw = restClient
                    .post()
                    .uri("/v1/projects/{projectId}/assessments?key={apiKey}", projectId, apiKey)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);
        } catch (Exception e) {
            log.warn("reCAPTCHA 검증 호출 실패, fail-open으로 허용: {}", e.getMessage());
            return true;
        }
        if (raw == null) {
            log.warn("reCAPTCHA 응답이 비어있음, fail-open으로 허용");
            return true;
        }

        JsonNode parsed = jsonMapper.readTree(raw);
        boolean valid = parsed.path("tokenProperties").path("valid").booleanValue(false);
        double score = parsed.path("riskAnalysis").path("score").doubleValue(0.0);
        return valid && score >= minScore;
    }
}
