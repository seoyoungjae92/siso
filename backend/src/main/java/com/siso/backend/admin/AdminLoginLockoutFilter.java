package com.siso.backend.admin;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * X-Client-Ip 같은 전달 헤더는 공격자가 요청마다 임의로 바꿔서 잠금을
 * 우회할 수 있으므로 절대 쓰지 않는다 — request.getRemoteAddr()(TCP
 * 연결의 실제 발신 IP, 위조 불가)만 신뢰한다.
 */
public class AdminLoginLockoutFilter extends OncePerRequestFilter {

    private final AdminLoginAttemptGuard guard;

    public AdminLoginLockoutFilter(AdminLoginAttemptGuard guard) {
        this.guard = guard;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (guard.isLockedOut(request.getRemoteAddr())) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"message\":\"로그인 시도가 너무 많습니다. 잠시 후 다시 시도해주세요.\"}");
            return;
        }
        chain.doFilter(request, response);
    }
}
