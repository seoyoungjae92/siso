package com.siso.backend.anon;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

public final class AnonIdHeader {

    private AnonIdHeader() {
    }

    public static UUID parse(String anonId, boolean required) {
        if (anonId == null || anonId.isBlank()) {
            if (required) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "X-Anon-Id header is required");
            }
            return null;
        }
        try {
            return UUID.fromString(anonId);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "X-Anon-Id must be a UUID");
        }
    }

    /** 쓰기 엔드포인트 전용 — anonId가 진짜 그 쿠키를 가진 프론트가 보낸 값인지
     * X-Anon-Sig 서명까지 검증한다. 서명 없이는 아무 UUID나 위조해 레이트리밋 등을
     * 우회할 수 있으므로 required=true인 모든 곳은 이걸 써야 한다. */
    public static UUID parseAndVerify(String anonId, String signature, AnonIdSigner signer) {
        UUID parsed = parse(anonId, true);
        if (!signer.verify(anonId, signature)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "X-Anon-Sig header missing or invalid");
        }
        return parsed;
    }
}
