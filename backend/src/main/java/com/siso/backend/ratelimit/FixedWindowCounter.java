package com.siso.backend.ratelimit;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Duration;
import java.util.List;

/**
 * 고정 윈도 카운터의 INCR와 만료 설정을 Lua 한 번으로 묶는다. 예전엔
 * increment 후 값이 1일 때만 expire를 따로 보내서, 그 사이에 연결이 끊기면
 * 만료 없는 키가 남아 그 사용자는 한도에 영원히 걸렸다. TTL이 없는 키를
 * 만나면 다시 걸어서 예전 버그로 남은 키도 스스로 풀린다. Upstash 과금
 * 기준으로도 명령 2개가 1개(EVAL)로 준다.
 */
public final class FixedWindowCounter {

    static final RedisScript<Long> INCR_WITH_WINDOW = new DefaultRedisScript<>(
            "local c = redis.call('INCR', KEYS[1]) "
                    + "if c == 1 or redis.call('PTTL', KEYS[1]) < 0 then "
                    + "redis.call('PEXPIRE', KEYS[1], ARGV[1]) end "
                    + "return c",
            Long.class);

    private FixedWindowCounter() {
    }

    public static long increment(StringRedisTemplate redisTemplate, String key, Duration window) {
        Long count = redisTemplate.execute(INCR_WITH_WINDOW, List.of(key), String.valueOf(window.toMillis()));
        return count == null ? 0L : count;
    }
}
