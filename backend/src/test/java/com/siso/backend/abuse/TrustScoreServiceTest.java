package com.siso.backend.abuse;

import com.siso.backend.alert.AdminAlert;
import com.siso.backend.alert.AdminAlertRepository;
import com.siso.backend.anon.AnonUser;
import com.siso.backend.anon.AnonUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrustScoreServiceTest {

    @Mock
    private AnonUserRepository anonUserRepository;

    @Mock
    private AdminAlertRepository adminAlertRepository;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private TrustScoreService newService() {
        return new TrustScoreService(anonUserRepository, adminAlertRepository, redisTemplate);
    }

    private AnonUser anonUser(OffsetDateTime firstSeen) {
        return new AnonUser(UUID.randomUUID(), firstSeen, "iphash");
    }

    @Test
    void recalculate_belowThreshold_noPenaltyNoAlert() {
        OffsetDateTime now = OffsetDateTime.now();
        List<AnonUser> cluster = List.of(anonUser(now.minusHours(1)), anonUser(now.minusHours(1)));
        when(anonUserRepository.findByIpHashRecent("iphash")).thenReturn(cluster);

        newService().recalculateForIpCluster("iphash", now, 4, 0.3f, 72, 0.3f);

        verify(anonUserRepository).saveAll(cluster);
        verify(adminAlertRepository, never()).save(any(AdminAlert.class));
        // 4 미만이라 페널티 없이 성숙도만 반영 — 갓 생성돼 age=1h면 최소치에 가까움
        assertThat(cluster.get(0).getTrustScore()).isGreaterThanOrEqualTo(0.3f);
    }

    @Test
    void recalculate_atThreshold_penalizesWholeClusterAndRaisesAlert() {
        OffsetDateTime now = OffsetDateTime.now();
        List<AnonUser> cluster = List.of(
                anonUser(now.minusHours(200)), // 성숙(오래됨)
                anonUser(now.minusHours(1)), // 갓 생성
                anonUser(now.minusHours(1)),
                anonUser(now.minusHours(1)));
        when(anonUserRepository.findByIpHashRecent("iphash")).thenReturn(cluster);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);

        newService().recalculateForIpCluster("iphash", now, 4, 0.3f, 72, 0.3f);

        verify(anonUserRepository).saveAll(cluster);
        // 성숙한 계정(maturity=1.0)도 페널티 곱해져서 1.0을 넘지 못함
        assertThat(cluster.get(0).getTrustScore()).isEqualTo(1.0f * 0.3f);

        verify(adminAlertRepository).save(any(AdminAlert.class));
    }

    @Test
    void recalculate_secondCallWithinDedupWindow_doesNotRaiseDuplicateAlert() {
        OffsetDateTime now = OffsetDateTime.now();
        List<AnonUser> cluster = List.of(
                anonUser(now.minusHours(1)), anonUser(now.minusHours(1)),
                anonUser(now.minusHours(1)), anonUser(now.minusHours(1)));
        when(anonUserRepository.findByIpHashRecent("iphash")).thenReturn(cluster);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(false);

        newService().recalculateForIpCluster("iphash", now, 4, 0.3f, 72, 0.3f);

        verify(adminAlertRepository, never()).save(any(AdminAlert.class));
    }

    @Test
    void recalculate_nullIpHash_isNoOp() {
        newService().recalculateForIpCluster(null, OffsetDateTime.now(), 4, 0.3f, 72, 0.3f);

        verify(anonUserRepository, never()).findByIpHashRecent(anyString());
    }
}
