package com.siso.backend.abuse;

import com.siso.backend.alert.AdminAlert;
import com.siso.backend.alert.AdminAlertRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * RateLimiter와 동일한 고정 윈도(fixed window) increment+TTL 패턴을
 * 재사용 — 다만 한도를 넘으면 막는 게 아니라, 카운트가 임계값에 "정확히
 * 도달한 시점"에 어드민 알림을 한 번만 남긴다(그 이후 매 요청마다 계속
 * 알림이 쌓이지 않도록).
 */
@Component
public class SpikeDetector {

    private final StringRedisTemplate redisTemplate;
    private final AdminAlertRepository adminAlertRepository;

    public SpikeDetector(StringRedisTemplate redisTemplate, AdminAlertRepository adminAlertRepository) {
        this.redisTemplate = redisTemplate;
        this.adminAlertRepository = adminAlertRepository;
    }

    public void recordVoteAndCheck(Long pairId, int threshold, int windowMinutes) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("kind", "vote");
        payload.put("pairId", pairId);
        check("spike:vote:" + pairId, threshold, windowMinutes, payload);
    }

    public void recordReactionUpAndCheck(Long commentId, int threshold, int windowMinutes) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("kind", "reaction_up");
        payload.put("commentId", commentId);
        check("spike:reaction_up:" + commentId, threshold, windowMinutes, payload);
    }

    private void check(String key, int threshold, int windowMinutes, Map<String, Object> payload) {
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redisTemplate.expire(key, Duration.ofMinutes(windowMinutes));
        }
        if (count != null && count == threshold) {
            payload.put("count", count);
            payload.put("windowMinutes", windowMinutes);
            adminAlertRepository.save(new AdminAlert("activity_spike", payload, OffsetDateTime.now()));
        }
    }
}
