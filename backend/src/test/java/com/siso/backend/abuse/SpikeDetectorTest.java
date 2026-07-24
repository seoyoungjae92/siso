package com.siso.backend.abuse;

import com.siso.backend.alert.AdminAlert;
import com.siso.backend.alert.AdminAlertRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpikeDetectorTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private AdminAlertRepository adminAlertRepository;

    private SpikeDetector newDetector() {
        return new SpikeDetector(redisTemplate, adminAlertRepository);
    }

    @Test
    void countReachesExactlyThreshold_raisesOneAlert() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(30L);

        newDetector().recordVoteAndCheck(1L, 30, 10);

        verify(adminAlertRepository).save(any(AdminAlert.class));
    }

    @Test
    void countBelowThreshold_noAlert() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(5L);

        newDetector().recordVoteAndCheck(1L, 30, 10);

        verify(adminAlertRepository, never()).save(any(AdminAlert.class));
    }

    @Test
    void countExceedsThresholdAfterAlreadyFired_doesNotRaiseAgain() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(31L);

        newDetector().recordVoteAndCheck(1L, 30, 10);

        verify(adminAlertRepository, never()).save(any(AdminAlert.class));
    }

    @Test
    void firstIncrement_setsExpiry() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(1L);

        newDetector().recordVoteAndCheck(1L, 30, 10);

        verify(redisTemplate, times(1)).expire(anyString(), any(Duration.class));
    }

    @Test
    void reactionSpike_usesCommentIdKey() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(30L);

        newDetector().recordReactionUpAndCheck(5L, 30, 10);

        verify(adminAlertRepository).save(any(AdminAlert.class));
    }
}
