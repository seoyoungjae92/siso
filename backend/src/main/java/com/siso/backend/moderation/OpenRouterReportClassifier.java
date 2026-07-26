package com.siso.backend.moderation;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * 신고된 댓글 1차 분류(D11) — OpenRouter(여러 AI 모델을 하나의 API로 제공하는
 * 서비스) 경유로 호출한다. 크롤러의 요약 재작성/주제 합성과 동일하게
 * openrouter/free를 기본값으로 쓰되(비용 최소화, CLAUDE.md §1.4), 모델은
 * moderation_settings.classification_model로 어드민이 언제든 특정 모델
 * 슬러그(예: anthropic/claude-haiku-4.5)로 바꿀 수 있다 — crawl_settings.
 * synthesis_model과 동일한 패턴.
 *
 * D14/D18: 신고된 댓글 본문과 신고 사유를 OpenRouter 및 그 경유 모델
 * 사업자(무료 모델 사용 시 호출마다 달라질 수 있음)로 전송하는 데이터
 * 흐름 — 개인정보처리방침 5절에 명시함.
 */
@Component
public class OpenRouterReportClassifier {

    private static final String BASE_URL = "https://openrouter.ai/api/v1";
    private static final int MAX_TOKENS = 300;
    private static final Duration TIMEOUT = Duration.ofSeconds(15);

    private static final String SYSTEM_PROMPT =
            """
            너는 익명 토론 커뮤니티에서 신고된 댓글을 1차로 검토하는 모더레이터야.
            아래 댓글 본문과 신고 사유들을 보고, 명백한 규정 위반인지 애매한 건인지
            판단해줘.

            규칙:
            1. 욕설·인신공격·혐오표현·명백한 스팸/도배처럼 누가 봐도 위반인 경우에만
               obvious_violation으로 분류해라.
            2. 단순히 의견이 강하거나 논쟁적이거나, 신고자가 동의하지 못하는 정치적
               주장이라는 이유만으로는 obvious_violation으로 분류하지 마라 — 그런
               건 ambiguous로 분류해서 사람이 판단하게 해라.
            3. 확신이 없으면 반드시 ambiguous로 분류해라 — 이 분류는 자동으로
               게시물을 삭제·차단하지 않는 참고용 힌트이므로, 과감하게 obvious로
               판단하기보다 신중하게 ambiguous로 판단하는 쪽이 안전하다.
            4. 반드시 요청된 스키마의 JSON 형식으로만 답해라. reason은 판단 근거를
               1~2문장 한국어로 간단히 설명해라.""";

    private static final Map<String, Object> RESPONSE_JSON_SCHEMA = Map.of(
            "type", "object",
            "properties", Map.of(
                    "verdict", Map.of("type", "string", "enum", List.of("obvious_violation", "ambiguous")),
                    "reason", Map.of("type", "string")),
            "required", List.of("verdict", "reason"),
            "additionalProperties", false);

    private final RestClient restClient;
    private final String apiKey;
    private final JsonMapper jsonMapper;

    public OpenRouterReportClassifier(
            RestClient.Builder builder, JsonMapper jsonMapper, @Value("${app.moderation.openrouter-api-key}") String apiKey) {
        HttpClientSettings settings =
                HttpClientSettings.defaults().withConnectTimeout(TIMEOUT).withReadTimeout(TIMEOUT);
        ClientHttpRequestFactory requestFactory = ClientHttpRequestFactoryBuilder.detect().build(settings);
        this.restClient = builder.baseUrl(BASE_URL).requestFactory(requestFactory).build();
        this.jsonMapper = jsonMapper;
        this.apiKey = apiKey;
    }

    public boolean isEnabled() {
        return apiKey != null && !apiKey.isBlank();
    }

    public ReportClassification classify(
            String model, String commentBody, List<String> reportReasons, List<String> reportDetails) {
        if (!isEnabled()) {
            throw new ReportClassificationFailed("OPENROUTER_API_KEY 미설정");
        }

        Map<String, Object> requestBody = Map.of(
                "model", model,
                "max_tokens", MAX_TOKENS,
                "messages", List.of(
                        Map.of("role", "system", "content", SYSTEM_PROMPT),
                        Map.of("role", "user", "content", buildUserPrompt(commentBody, reportReasons, reportDetails))),
                "response_format", Map.of(
                        "type", "json_schema",
                        "json_schema", Map.of(
                                "name", "report_classification", "strict", true, "schema", RESPONSE_JSON_SCHEMA)),
                "provider", Map.of("require_parameters", true));

        String raw;
        try {
            raw = restClient
                    .post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);
        } catch (Exception e) {
            throw new ReportClassificationFailed("OpenRouter 호출 실패: " + e.getMessage(), e);
        }
        if (raw == null) {
            throw new ReportClassificationFailed("OpenRouter 응답이 비어있음");
        }

        JsonNode root = jsonMapper.readTree(raw);
        JsonNode choice = root.path("choices").get(0);
        if (choice == null) {
            throw new ReportClassificationFailed("OpenRouter 응답 형식 이상: choices 없음");
        }
        String finishReason = choice.path("finish_reason").stringValue();
        if (!"stop".equals(finishReason)) {
            throw new ReportClassificationFailed("OpenRouter 응답 비정상 종료: finish_reason=" + finishReason);
        }

        String content = choice.path("message").path("content").stringValue();
        if (content == null) {
            throw new ReportClassificationFailed("OpenRouter 응답에 content 없음");
        }

        JsonNode parsed = jsonMapper.readTree(content);
        String verdict = parsed.path("verdict").stringValue();
        String reason = parsed.path("reason").stringValue();
        if (verdict == null || reason == null || reason.isBlank()) {
            throw new ReportClassificationFailed("응답에 verdict/reason 필드 누락");
        }
        return new ReportClassification(verdict, reason);
    }

    private String buildUserPrompt(String commentBody, List<String> reportReasons, List<String> reportDetails) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("[신고된 댓글]\n").append(commentBody).append("\n\n[신고 사유]\n");
        for (String reason : reportReasons) {
            prompt.append("- ").append(reason).append('\n');
        }
        if (!reportDetails.isEmpty()) {
            prompt.append("\n[신고 상세 사유]\n");
            for (String detail : reportDetails) {
                prompt.append("- ").append(detail).append('\n');
            }
        }
        return prompt.toString();
    }
}
