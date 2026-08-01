package com.siso.backend.web;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleResponseStatus_keepsOriginalStatusAndReason() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleResponseStatus(new ResponseStatusException(HttpStatus.NOT_FOUND, "pair not found"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsEntry("message", "pair not found");
    }

    @Test
    void handleValidation_returns400WithoutLeakingFieldDetails() {
        MethodArgumentNotValidException ex =
                new MethodArgumentNotValidException((org.springframework.core.MethodParameter) null, mock(BindingResult.class));

        ResponseEntity<Map<String, Object>> response = handler.handleValidation(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("message", "입력값이 올바르지 않습니다. 다시 확인해주세요.");
    }

    @Test
    void handleUnexpected_sanitizesArbitraryException() {
        // 필수 필드 누락 등으로 NPE가 나면 Redis 호스트 같은 내부 정보가
        // 그대로 노출되던 버그(감사에서 발견) — 어떤 예외든 일반 메시지로
        // 치환돼야 한다.
        ResponseEntity<Map<String, Object>> response =
                handler.handleUnexpected(new NullPointerException("Cannot invoke on redis://internal-host:6379"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).containsEntry("message", "일시적인 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");
        assertThat(response.getBody().toString()).doesNotContain("redis://internal-host");
    }
}
