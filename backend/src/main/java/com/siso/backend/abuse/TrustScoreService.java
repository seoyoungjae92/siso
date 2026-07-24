package com.siso.backend.abuse;

import com.siso.backend.alert.AdminAlert;
import com.siso.backend.alert.AdminAlertRepository;
import com.siso.backend.anon.AnonUser;
import com.siso.backend.anon.AnonUserRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 신뢰도 점수는 증분으로 가감하지 않고 호출 시점마다 전체를 다시
 * 계산한다 — 순서에 따라 값이 어긋나는(드리프트) 문제를 피하기 위함.
 * 가입 후 경과 시간 기반으로 서서히 성숙(maturity)하다가, 같은 IP
 * 해시를 쓰는 익명 ID가 임계값 이상 모이면 그 클러스터 전원에게
 * 페널티 배수를 곱한다(새로 걸린 계정만이 아니라 클러스터 전체 —
 * 안 그러면 먼저 있던 계정들은 빠져나감).
 */
@Service
public class TrustScoreService {

    private static final String ALERT_MARKER_PREFIX = "abuse-alert:multi_account_same_ip:";
    private static final Duration ALERT_DEDUP_WINDOW = Duration.ofHours(24);

    private final AnonUserRepository anonUserRepository;
    private final AdminAlertRepository adminAlertRepository;
    private final StringRedisTemplate redisTemplate;

    public TrustScoreService(
            AnonUserRepository anonUserRepository,
            AdminAlertRepository adminAlertRepository,
            StringRedisTemplate redisTemplate) {
        this.anonUserRepository = anonUserRepository;
        this.adminAlertRepository = adminAlertRepository;
        this.redisTemplate = redisTemplate;
    }

    @Transactional
    public void recalculateForIpCluster(
            String ipHash,
            OffsetDateTime now,
            int clusterSizeThreshold,
            float penaltyMultiplier,
            int maturityHours,
            float minWeight) {
        if (ipHash == null) {
            return;
        }

        List<AnonUser> cluster = anonUserRepository.findByIpHashRecent(ipHash);
        boolean penalized = cluster.size() >= clusterSizeThreshold;

        for (AnonUser anonUser : cluster) {
            float maturity = maturityWeight(anonUser.getFirstSeen(), now, maturityHours, minWeight);
            anonUser.setTrustScore(penalized ? maturity * penaltyMultiplier : maturity);
        }
        anonUserRepository.saveAll(cluster);

        if (penalized) {
            raiseAlertOnce(ipHash, cluster, now);
        }
    }

    private float maturityWeight(OffsetDateTime firstSeen, OffsetDateTime now, int maturityHours, float minWeight) {
        long ageHours = Duration.between(firstSeen, now).toHours();
        float progress = Math.min(1.0f, (float) ageHours / maturityHours);
        return minWeight + (1.0f - minWeight) * progress;
    }

    private void raiseAlertOnce(String ipHash, List<AnonUser> cluster, OffsetDateTime now) {
        String marker = ALERT_MARKER_PREFIX + ipHash;
        Boolean firstTime = redisTemplate.opsForValue().setIfAbsent(marker, "1", ALERT_DEDUP_WINDOW);
        if (!Boolean.TRUE.equals(firstTime)) {
            return;
        }

        List<UUID> anonIds = cluster.stream().map(AnonUser::getAnonId).collect(Collectors.toList());
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("ipHash", ipHash);
        payload.put("anonIds", anonIds);
        payload.put("count", cluster.size());
        adminAlertRepository.save(new AdminAlert("multi_account_same_ip", payload, now));
    }
}
