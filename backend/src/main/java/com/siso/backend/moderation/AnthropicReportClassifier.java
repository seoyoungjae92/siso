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
 * 신고된 댓글 1차 분류(D11) — Claude Haiku(Anthropic Messages API)를 직접
 * 호출한다. 크롤러의 OpenRouter 호출과 마찬가지로 별도 SDK 없이 raw HTTP로
 * 호출(AssemblyPetitionClient와 동일한 패턴) — 이 코드베이스 전체에서
 * 외부 LLM/API 호출에 프로바이더 SDK를 쓰지 않는 기존 관행을 따름.
 *
 * D14: 신고된 댓글 본문과 신고 사유를 해외 사업자(Anthropic)로 전송하는
 * 데이터 흐름 — 개인정보처리방침 5절에 명시함.
 */
@Component
public class AnthropicReportClassifier {

    private static final String BASE_URL = "https://api.anthropic.com";
    private static final String ANTHROPIC_VERSION = "2023-06-01";
    private static final String MODEL = "claude-haiku-4-5";
    private static final int MAX_TOKENS = 300;
    private static final Duration TIMEOUT = Duration.ofSeconds(15);
    private static final String TOOL_NAME = "classify_report";

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
            4. reason은 판단 근거를 1~2문장 한국어로 간단히 설명해라.""";

    private final RestClient restClient;
    private final String apiKey;
    private final JsonMapper jsonMapper;

    public AnthropicReportClassifier(
            RestClient.Builder builder, JsonMapper jsonMapper, @Value("${app.moderation.anthropic-api-key}") String apiKey) {
        HttpClientSettings settings =
                HttpClientSettings.defaults().withConnectTimeout(TIMEOUT).withReadTimeout(TIMEOUT);
        ClientHttpRequestFactory requestFactory = ClientHttpRequestFactoryBuilder.detect().build(settings);
        this.restClient = builder
                .baseUrl(BASE_URL)
                .requestFactory(requestFactory)
                .defaultHeader("x-api-key", apiKey)
                .defaultHeader("anthropic-version", ANTHROPIC_VERSION)
                .build();
        this.jsonMapper = jsonMapper;
        this.apiKey = apiKey;
    }

    public boolean isEnabled() {
        return apiKey != null && !apiKey.isBlank();
    }

    public ReportClassification classify(String commentBody, List<String> reportReasons, List<String> reportDetails) {
        if (!isEnabled()) {
            throw new ReportClassificationFailed("ANTHROPIC_API_KEY 미설정");
        }

        Map<String, Object> requestBody = Map.of(
                "model", MODEL,
                "max_tokens", MAX_TOKENS,
                "system", SYSTEM_PROMPT,
                "messages", List.of(Map.of("role", "user", "content", buildUserPrompt(commentBody, reportReasons, reportDetails))),
                "tools", List.of(Map.of(
                        "name", TOOL_NAME,
                        "description", "신고된 댓글의 1차 분류 결과",
                        "input_schema", Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "verdict",
                                        Map.of("type", "string", "enum", List.of("obvious_violation", "ambiguous")),
                                        "reason", Map.of("type", "string")),
                                "required", List.of("verdict", "reason")))),
                "tool_choice", Map.of("type", "tool", "name", TOOL_NAME));

        String raw;
        try {
            raw = restClient.post().uri("/v1/messages").body(requestBody).retrieve().body(String.class);
        } catch (Exception e) {
            throw new ReportClassificationFailed("Anthropic API 호출 실패: " + e.getMessage(), e);
        }
        if (raw == null) {
            throw new ReportClassificationFailed("Anthropic 응답이 비어있음");
        }

        JsonNode root = jsonMapper.readTree(raw);
        for (JsonNode block : root.path("content")) {
            if (!"tool_use".equals(block.path("type").stringValue())
                    || !TOOL_NAME.equals(block.path("name").stringValue())) {
                continue;
            }
            JsonNode input = block.path("input");
            String verdict = input.path("verdict").stringValue();
            String reason = input.path("reason").stringValue();
            if (verdict == null || reason == null) {
                throw new ReportClassificationFailed("응답에 verdict/reason 필드 누락");
            }
            return new ReportClassification(verdict, reason);
        }
        throw new ReportClassificationFailed("tool_use 응답 블록을 찾을 수 없음");
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
