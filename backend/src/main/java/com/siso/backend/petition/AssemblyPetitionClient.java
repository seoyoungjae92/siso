package com.siso.backend.petition;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * 국회 열린국회정보 Open API(PTTRCP, 청원 접수목록) 클라이언트. User-Agent
 * 헤더가 없으면 WAF가 무조건 400을 반환한다는 걸 실제 호출로 확인함 —
 * 어떤 값이든 있으면 통과하지만, 크롤러(fetch.py)와 동일하게 정직한 서비스
 * 식별 UA를 사용한다(가짜 브라우저 UA 흉내 안 냄). 또한 이 API는 Type=json
 * 요청에도 응답 Content-Type을 text/html로 잘못 내려줘서(실측 확인) body를
 * JsonNode.class로 바로 바인딩하면 content-type 불일치로 실패한다 — String
 * 으로 받아서 직접 파싱한다.
 */
@Component
public class AssemblyPetitionClient {

    private static final String BASE_URL = "https://open.assembly.go.kr/portal/openapi/PTTRCP";
    private static final String USER_AGENT = "siso-backend/0.1 (+contact: TODO-set-before-launch@example.com)";
    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private final RestClient restClient;
    private final String apiKey;
    private final JsonMapper jsonMapper;

    public AssemblyPetitionClient(
            RestClient.Builder builder, JsonMapper jsonMapper, @Value("${app.assembly.api-key}") String apiKey) {
        HttpClientSettings settings =
                HttpClientSettings.defaults().withConnectTimeout(TIMEOUT).withReadTimeout(TIMEOUT);
        ClientHttpRequestFactory requestFactory = ClientHttpRequestFactoryBuilder.detect().build(settings);
        this.restClient = builder
                .baseUrl(BASE_URL)
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.USER_AGENT, USER_AGENT)
                .build();
        this.jsonMapper = jsonMapper;
        this.apiKey = apiKey;
    }

    public List<AssemblyPetitionRow> fetchList(String eraco, int pageSize) {
        String raw = restClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .queryParam("KEY", apiKey)
                        .queryParam("Type", "json")
                        .queryParam("pIndex", 1)
                        .queryParam("pSize", pageSize)
                        .queryParam("ERACO", eraco)
                        .build())
                .retrieve()
                .body(String.class);

        List<AssemblyPetitionRow> rows = new ArrayList<>();
        if (raw == null) {
            return rows;
        }
        JsonNode root = jsonMapper.readTree(raw);
        for (JsonNode section : root.path("PTTRCP")) {
            JsonNode rowArray = section.path("row");
            if (!rowArray.isArray()) {
                continue;
            }
            for (JsonNode row : rowArray) {
                rows.add(new AssemblyPetitionRow(
                        row.path("PTT_ID").stringValue(),
                        row.path("PTT_NM").stringValue(),
                        row.path("PTT_KIND").stringValue(),
                        row.path("CITZN_AGM_CNT").stringValue(),
                        row.path("RCP_DT").stringValue(),
                        row.path("LINK_URL").stringValue()));
            }
        }
        return rows;
    }
}
