package com.siso.backend.moderation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;

/**
 * 댓글 작성 시 reCAPTCHA v3 점수 기반 검증(§6). RECAPTCHA_SECRET_KEY가
 * 비어있으면(미설정) 검증 자체를 건너뛴다 — 다른 선택적 외부 연동
 * (OPENROUTER_API_KEY 등)과 동일한 패턴. Google 쪽 장애·네트워크 오류 시엔
 * fail-open(허용)한다 — CAPTCHA는 보조 방어선일 뿐이라 이것 때문에 댓글
 * 작성 자체가 막히면 안 된다. 반대로 검증에 정상 도달했는데 점수가
 * 낮거나 success=false면 fail-closed(차단)한다.
 */
@Component
public class RecaptchaVerifier {

    private static final Logger log = LoggerFactory.getLogger(RecaptchaVerifier.class);
    private static final String VERIFY_URL = "https://www.google.com/recaptcha/api/siteverify";
    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private final RestClient restClient;
    private final JsonMapper jsonMapper;
    private final String secretKey;
    private final double minScore;

    public RecaptchaVerifier(
            RestClient.Builder builder,
            JsonMapper jsonMapper,
            @Value("${app.recaptcha.secret-key:}") String secretKey,
            @Value("${app.recaptcha.min-score:0.5}") double minScore) {
        HttpClientSettings settings =
                HttpClientSettings.defaults().withConnectTimeout(TIMEOUT).withReadTimeout(TIMEOUT);
        ClientHttpRequestFactory requestFactory = ClientHttpRequestFactoryBuilder.detect().build(settings);
        this.restClient = builder.requestFactory(requestFactory).build();
        this.jsonMapper = jsonMapper;
        this.secretKey = secretKey;
        this.minScore = minScore;
    }

    public boolean isEnabled() {
        return secretKey != null && !secretKey.isBlank();
    }

    public boolean verify(String token) {
        if (!isEnabled()) {
            return true;
        }
        if (token == null || token.isBlank()) {
            return false;
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("secret", secretKey);
        form.add("response", token);

        String raw;
        try {
            raw = restClient.post().uri(VERIFY_URL).body(form).retrieve().body(String.class);
        } catch (Exception e) {
            log.warn("reCAPTCHA 검증 호출 실패, fail-open으로 허용: {}", e.getMessage());
            return true;
        }
        if (raw == null) {
            log.warn("reCAPTCHA 응답이 비어있음, fail-open으로 허용");
            return true;
        }

        JsonNode parsed = jsonMapper.readTree(raw);
        boolean success = parsed.path("success").booleanValue(false);
        double score = parsed.path("score").doubleValue(0.0);
        return success && score >= minScore;
    }
}
