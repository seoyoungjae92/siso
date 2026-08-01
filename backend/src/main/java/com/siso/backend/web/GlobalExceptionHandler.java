package com.siso.backend.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

/**
 * ResponseStatusException(의도적으로 사용자에게 보여줄 한글 메시지로 던진
 * 것들)은 원래 응답 그대로 유지하고, 그 외 처리 안 된 예외(NPE 등)는
 * spring.web.error.include-message=always 설정 때문에 원본 예외 메시지가
 * (Redis 호스트 등 내부 인프라 정보까지) 그대로 노출되는 걸 막는다 —
 * 실제로 필수 필드 누락 시 NPE로 인증 없는 공개 API가 내부 정보를
 * 노출하는 걸 감사에서 확인함.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatus(ResponseStatusException ex) {
        String message = ex.getReason() != null ? ex.getReason() : ex.getStatusCode().toString();
        return ResponseEntity.status(ex.getStatusCode()).body(Map.of("message", message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpected(Exception ex) {
        log.error("처리되지 않은 예외", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "일시적인 오류가 발생했습니다. 잠시 후 다시 시도해주세요."));
    }
}
