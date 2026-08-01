package com.siso.backend.admin;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 관리자 CMS Basic Auth엔 브루트포스 방어가 전혀 없어서(감사에서 지적됨),
 * 자격증명 하나만 뚫리면 지역 제약 없이 CMS 전체가 열리는 상태였음. IP당
 * 실패 횟수를 세어 임계값 넘으면 일정 시간 잠근다. 자주 튜닝할 값이 아니라
 * 다른 어뷰징 임계값들과 달리 어드민 설정 테이블에 넣지 않고 상수로 둠.
 */
@Component
public class AdminLoginAttemptGuard {

    private static final String KEY_PREFIX = "admin-login-fail:";
    static final int MAX_ATTEMPTS = 5;
    static final Duration LOCKOUT_WINDOW = Duration.ofMinutes(15);

    private final StringRedisTemplate redisTemplate;

    public AdminLoginAttemptGuard(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean isLockedOut(String ip) {
        String value = redisTemplate.opsForValue().get(KEY_PREFIX + ip);
        return value != null && Integer.parseInt(value) >= MAX_ATTEMPTS;
    }

    public void recordFailure(String ip) {
        String key = KEY_PREFIX + ip;
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redisTemplate.expire(key, LOCKOUT_WINDOW);
        }
    }

    public void recordSuccess(String ip) {
        redisTemplate.delete(KEY_PREFIX + ip);
    }
}
