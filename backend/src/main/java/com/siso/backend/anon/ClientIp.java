package com.siso.backend.anon;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 프론트가 Server Action(Vercel 서버)에서 백엔드를 호출하는 구조라
 * request.getRemoteAddr()는 항상 Vercel 서버 IP만 보여준다(같은 IP 클러스터
 * 탐지가 무력화됨). 프론트가 자기가 받은 실제 방문자 IP(x-forwarded-for)를
 * X-Client-Ip로 넘겨주면 그걸 우선 쓰고, 없으면(직접 API 호출 등) remoteAddr로
 * 폴백한다.
 *
 * X-Client-Ip는 X-Client-Ip-Sig 서명이 맞을 때만 믿는다. 예전엔 헤더를 그대로
 * 믿어서 백엔드를 직접 부르면 IP를 마음대로 바꿔 IP 클러스터 탐지를 피할 수
 * 있었다. 서명은 익명 ID와 같은 비밀키로, 익명 ID 서명과 섞이지 않게 "ip:"를
 * 붙여 만든다.
 */
public final class ClientIp {

    static final String SIGN_PREFIX = "ip:";

    private ClientIp() {
    }

    public static String resolve(HttpServletRequest request, AnonIdSigner signer) {
        String forwarded = request.getHeader("X-Client-Ip");
        if (forwarded != null && !forwarded.isBlank()
                && signer.verify(SIGN_PREFIX + forwarded, request.getHeader("X-Client-Ip-Sig"))) {
            return forwarded;
        }
        return request.getRemoteAddr();
    }
}
