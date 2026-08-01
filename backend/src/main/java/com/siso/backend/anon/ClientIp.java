package com.siso.backend.anon;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 프론트가 Server Action(Vercel 서버)에서 백엔드를 호출하는 구조라
 * request.getRemoteAddr()는 항상 Vercel 서버 IP만 보여준다(같은 IP 클러스터
 * 탐지가 무력화됨). 프론트가 자기가 받은 실제 방문자 IP(x-forwarded-for)를
 * X-Client-Ip로 넘겨주면 그걸 우선 쓰고, 없으면(직접 API 호출 등) remoteAddr로
 * 폴백한다.
 */
public final class ClientIp {

    private ClientIp() {
    }

    public static String resolve(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Client-Ip");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded;
        }
        return request.getRemoteAddr();
    }
}
