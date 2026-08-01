package com.siso.backend.anon;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.InvalidKeyException;
import java.util.HexFormat;

/**
 * X-Anon-Id 헤더는 클라이언트가 그대로 실어 보내는 값이라 서명 없이는 아무 UUID나
 * 위조 가능(레이트리밋/중복필터/투표가중치가 전부 이 값 기준). 프론트가 쿠키값으로
 * 서명을 계산해 X-Anon-Sig로 같이 보내고, 비밀키를 모르면 유효한 서명을 만들 수 없다.
 */
@Component
public class AnonIdSigner {

    private final String secret;

    public AnonIdSigner(@Value("${app.anon.signing-secret}") String secret) {
        this.secret = secret;
    }

    public String sign(String anonId) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] signed = mac.doFinal(anonId.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(signed);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("HmacSHA256 서명 실패", e);
        }
    }

    public boolean verify(String anonId, String signature) {
        if (signature == null) {
            return false;
        }
        return MessageDigest.isEqual(
                sign(anonId).getBytes(StandardCharsets.UTF_8), signature.getBytes(StandardCharsets.UTF_8));
    }
}
