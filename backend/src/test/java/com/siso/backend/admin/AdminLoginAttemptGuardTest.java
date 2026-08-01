package com.siso.backend.admin;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminLoginAttemptGuardTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private AdminLoginAttemptGuard newGuard() {
        return new AdminLoginAttemptGuard(redisTemplate);
    }

    @Test
    void isLockedOut_belowThreshold_returnsFalse() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("admin-login-fail:1.2.3.4")).thenReturn("4");

        assertThat(newGuard().isLockedOut("1.2.3.4")).isFalse();
    }

    @Test
    void isLockedOut_atThreshold_returnsTrue() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("admin-login-fail:1.2.3.4"))
                .thenReturn(String.valueOf(AdminLoginAttemptGuard.MAX_ATTEMPTS));

        assertThat(newGuard().isLockedOut("1.2.3.4")).isTrue();
    }

    @Test
    void isLockedOut_noPriorFailures_returnsFalse() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("admin-login-fail:1.2.3.4")).thenReturn(null);

        assertThat(newGuard().isLockedOut("1.2.3.4")).isFalse();
    }

    @Test
    void recordFailure_firstAttempt_setsExpiry() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(1L);

        newGuard().recordFailure("1.2.3.4");

        verify(redisTemplate, times(1))
                .expire("admin-login-fail:1.2.3.4", AdminLoginAttemptGuard.LOCKOUT_WINDOW);
    }

    @Test
    void recordFailure_subsequentAttempt_doesNotResetExpiry() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(2L);

        newGuard().recordFailure("1.2.3.4");

        verify(redisTemplate, never()).expire(anyString(), any(Duration.class));
    }

    @Test
    void recordSuccess_clearsCounter() {
        newGuard().recordSuccess("1.2.3.4");

        verify(redisTemplate).delete("admin-login-fail:1.2.3.4");
    }
}
