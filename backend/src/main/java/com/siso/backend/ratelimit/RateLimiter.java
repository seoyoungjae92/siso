package com.siso.backend.ratelimit;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.UUID;

/**
 * 고정 윈도(fixed window) 카운터 — 슬라이딩 윈도가 아니라 윈도 경계에서
 * 순간적으로 2배까지 허용될 수 있는 단순한 구현. 5절 스펙에 정밀한 윈도
 * 요구가 없어 MVP 수준으로 충분하다고 판단.
 *
 * 5절: "익명 ID당 댓글 분당 3개/시간당 20개, 투표·추천도 유사 제한" —
 * 별도 수치가 없는 투표/추천도 댓글과 동일 기준으로 시작.
 */
@Component
public class RateLimiter {

    private static final long PER_MINUTE_LIMIT = 3;
    private static final long PER_HOUR_LIMIT = 20;

    private final StringRedisTemplate redisTemplate;

    public RateLimiter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void checkOrThrow(String action, UUID anonId) {
        boolean withinMinute = tryConsume(
                "ratelimit:" + action + ":min:" + anonId, PER_MINUTE_LIMIT, Duration.ofMinutes(1));
        boolean withinHour = tryConsume(
                "ratelimit:" + action + ":hour:" + anonId, PER_HOUR_LIMIT, Duration.ofHours(1));

        if (!withinMinute || !withinHour) {
            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS, "너무 자주 요청했습니다. 잠시 후 다시 시도해주세요.");
        }
    }

    private boolean tryConsume(String key, long limit, Duration window) {
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redisTemplate.expire(key, window);
        }
        return count != null && count <= limit;
    }
}
