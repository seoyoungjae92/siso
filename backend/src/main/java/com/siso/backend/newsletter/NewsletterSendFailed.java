package com.siso.backend.newsletter;

/**
 * 이메일 발송 API 에러 등 모든 실패를 이 예외 하나로 감싼다 — 호출부는
 * 해당 건을 건너뛰고 계속 진행한다(다른 구독자에게는 계속 발송).
 */
public class NewsletterSendFailed extends RuntimeException {

    public NewsletterSendFailed(String message) {
        super(message);
    }

    public NewsletterSendFailed(String message, Throwable cause) {
        super(message, cause);
    }
}
