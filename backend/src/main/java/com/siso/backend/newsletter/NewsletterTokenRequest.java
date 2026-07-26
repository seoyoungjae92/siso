package com.siso.backend.newsletter;

// UUID로 바로 바인딩하면 형식이 안 맞는 토큰이 왔을 때 Spring/Jackson의
// 원시 파싱 예외 메시지가 그대로 사용자에게 노출된다(실측으로 확인) —
// String으로 받아서 서비스 계층에서 직접 검증하고 깔끔한 에러로 변환한다.
public record NewsletterTokenRequest(String token) {
}
