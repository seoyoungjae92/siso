package com.siso.backend.moderation;

/**
 * API 에러, 응답 형식 이상, 필드 누락 등 모든 실패를 이 예외 하나로 감싼다 —
 * 호출부(ReportClassificationService)는 해당 댓글을 건너뛰고 다음 배치에서
 * 다시 시도한다(fail-open, 자동 조치 없는 힌트 기능이라 판단 실패로 글을
 * 방치하는 게 안전).
 */
public class ReportClassificationFailed extends RuntimeException {

    public ReportClassificationFailed(String message) {
        super(message);
    }

    public ReportClassificationFailed(String message, Throwable cause) {
        super(message, cause);
    }
}
